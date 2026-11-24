package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.CertaintyCalculator
import com.cropdoctor.domain.DiagnosisOutcome
import com.cropdoctor.domain.ScoredClass
import kotlin.math.exp
import kotlin.math.ln

data class DiagnosisEvaluation(
    val classId: String,
    val displayName: String,
    val confidence: Float,
    val certaintyBand: CertaintyBand,
    val top3: List<ScoredClass>,
    val freeEnergy: Double,
    val isOutOfDistribution: Boolean,
    val isBelowThreshold: Boolean,
    val outcome: DiagnosisOutcome = DiagnosisOutcome.DIAGNOSE,
    val outcomeReason: String = "",
    val margin: Float = 0.0f
)

object VisionMath {

    const val MODEL_OUTPUT_SIZE = 15

    /**
     * Applies temperature scaling to raw logits: z'_i = z_i / T
     */
    fun applyTemperature(logits: FloatArray, temperature: Float): FloatArray {
        val t = if (temperature > 0f) temperature else 1.0f
        val scaled = FloatArray(logits.size)
        for (i in logits.indices) {
            scaled[i] = logits[i] / t
        }
        return scaled
    }

    /**
     * Numerically stable Free Energy calculation: E(x; T) = -T * ln(sum(exp(z_i / T)))
     */
    fun computeFreeEnergy(scaledLogits: FloatArray, temperature: Float): Double {
        if (scaledLogits.isEmpty()) return 0.0
        val t = if (temperature > 0f) temperature.toDouble() else 1.0

        var maxLogit = scaledLogits[0]
        for (v in scaledLogits) {
            if (v > maxLogit) maxLogit = v
        }

        var sumExp = 0.0
        for (v in scaledLogits) {
            sumExp += exp((v - maxLogit).toDouble())
        }

        val logSumExp = maxLogit.toDouble() + ln(sumExp)
        return -t * logSumExp
    }

    /**
     * Numerically stable softmax over an entire float array.
     */
    fun softmax(logits: FloatArray): FloatArray {
        if (logits.isEmpty()) return FloatArray(0)
        var maxVal = logits[0]
        for (v in logits) if (v > maxVal) maxVal = v

        val expVals = FloatArray(logits.size)
        var sum = 0.0f
        for (i in logits.indices) {
            expVals[i] = exp(logits[i] - maxVal)
            sum += expVals[i]
        }
        if (sum > 0f) {
            for (i in expVals.indices) {
                expVals[i] /= sum
            }
        }
        return expVals
    }

    data class CropMaskResult(
        val probabilities: FloatArray,
        val cropMass: Float
    )

    /**
     * Crop chip masking: when a crop is chosen, keep only that crop's classes and re-normalise.
     * Also returns cropMass: total probability the model gave to the selected crop's classes.
     * "Auto" / "all" / null uses all classes and has cropMass = 1.0f.
     */
    fun maskAndRenormalizeWithMass(
        labels: List<String>,
        scaledLogits: FloatArray,
        cropHint: String?
    ): CropMaskResult {
        val n = labels.size
        val probabilities = FloatArray(n) { 0.0f }
        if (n == 0 || scaledLogits.isEmpty()) return CropMaskResult(probabilities, 1.0f)

        // Compute global unmasked softmax probabilities to measure model's total mass assigned to crop
        val globalProbs = softmax(scaledLogits)

        val normalizedHint = cropHint?.lowercase()?.trim()
        val isAuto = normalizedHint == null || normalizedHint == "all" || normalizedHint == "auto" || normalizedHint.isEmpty()

        if (isAuto) {
            return CropMaskResult(globalProbs, 1.0f)
        }

        val prefix = "${normalizedHint}_"
        val matchingIndices = labels.indices.filter { labels[it].startsWith(prefix) }

        if (matchingIndices.isEmpty()) {
            return CropMaskResult(globalProbs, 1.0f)
        }

        var mass = 0.0f
        for (idx in matchingIndices) {
            if (idx < globalProbs.size) {
                mass += globalProbs[idx]
            }
        }

        var maxVal = Float.NEGATIVE_INFINITY
        for (idx in matchingIndices) {
            if (idx < scaledLogits.size && scaledLogits[idx] > maxVal) {
                maxVal = scaledLogits[idx]
            }
        }

        var sum = 0.0f
        for (idx in matchingIndices) {
            if (idx < scaledLogits.size) {
                val expVal = exp(scaledLogits[idx] - maxVal)
                probabilities[idx] = expVal
                sum += expVal
            }
        }

        if (sum > 0.0f) {
            for (idx in matchingIndices) {
                probabilities[idx] /= sum
            }
        }

        return CropMaskResult(probabilities, mass)
    }

    /**
     * Crop chip masking: when a crop is chosen, keep only that crop's classes and re-normalise.
     * "Auto" / "all" / null uses all classes.
     */
    fun maskAndRenormalize(
        labels: List<String>,
        scaledLogits: FloatArray,
        cropHint: String?
    ): FloatArray {
        return maskAndRenormalizeWithMass(labels, scaledLogits, cropHint).probabilities
    }

    /**
     * Extracts top-k classes sorted by probability in descending order.
     */
    fun extractTopK(labels: List<String>, probabilities: FloatArray, k: Int = 3): List<ScoredClass> {
        val scored = mutableListOf<ScoredClass>()
        val count = minOf(labels.size, probabilities.size)
        for (i in 0 until count) {
            val prob = probabilities[i]
            if (prob > 0.0f || count <= k) {
                val classId = labels[i]
                val displayName = formatDisplayName(classId)
                scored.add(ScoredClass(classId, displayName, prob))
            }
        }
        scored.sortByDescending { it.probability }
        return scored.take(k)
    }

    fun formatDisplayName(classId: String): String {
        return classId
            .split("_")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    /**
     * Evaluates diagnosis with temperature scaling, confidence thresholds, crop mass, margin, and OOD detection.
     * Implements Task P4: Three clear outcomes (DIAGNOSE, CANT_TELL, OUT_OF_CLASS) in plain code.
     * Enforces fail closed.
     */
    fun evaluateDiagnosis(
        labels: List<String>,
        probabilities: FloatArray,
        thresholds: Map<String, Float> = emptyMap(),
        defaultThreshold: Float = 0.40f,
        freeEnergy: Double = 0.0,
        energyThreshold: Double? = null,
        unsupportedClasses: Set<String> = emptySet(),
        cropMass: Float = 1.0f,
        cropMassThreshold: Float = 0.40f,
        marginThreshold: Float = 0.15f,
        borderlineEnergy: Double = -9.0,
        gate1Pass: Boolean = true,
        leafValidatorReason: String = ""
    ): DiagnosisEvaluation {
        val topK = extractTopK(labels, probabilities, 3)
        val top1 = topK.firstOrNull()
        val top2 = if (topK.size > 1) topK[1] else null
        val top1Prob = top1?.probability ?: 0.0f
        val top2Prob = top2?.probability ?: 0.0f
        val margin = top1Prob - top2Prob

        val isWeeds = top1 != null && (top1.classId == "weeds_not_a_crop" || top1.classId == "other_not_a_crop")
        val isUnsupportedClass = top1 != null && unsupportedClasses.contains(top1.classId)
        val isOOD = if (energyThreshold != null) freeEnergy > energyThreshold else false
        val requiredThreshold = if (top1 != null) {
            thresholds[top1.classId] ?: defaultThreshold
        } else {
            defaultThreshold
        }
        val isBelowThreshold = top1 == null || top1Prob < requiredThreshold

        // Task P4 three clear outcomes
        val (outcome, outcomeReason) = when {
            !gate1Pass -> DiagnosisOutcome.CANT_TELL to "LEAF_VALIDATOR_FAIL($leafValidatorReason)"
            isWeeds -> DiagnosisOutcome.CANT_TELL to "GLOBAL_TOP1_WEEDS"
            isOOD -> DiagnosisOutcome.CANT_TELL to "HIGH_FREE_ENERGY($freeEnergy > $energyThreshold)"
            isBelowThreshold -> DiagnosisOutcome.CANT_TELL to "LOW_INCROP_CONFIDENCE($top1Prob < $requiredThreshold)"
            cropMass < cropMassThreshold -> DiagnosisOutcome.OUT_OF_CLASS to "LOW_CROP_MASS($cropMass < $cropMassThreshold)"
            margin < marginThreshold && freeEnergy >= borderlineEnergy ->
                DiagnosisOutcome.OUT_OF_CLASS to "BORDERLINE_AMBIGUOUS(margin=$margin < $marginThreshold, E=$freeEnergy >= $borderlineEnergy)"
            isUnsupportedClass -> DiagnosisOutcome.OUT_OF_CLASS to "UNSUPPORTED_CLASS(${top1?.classId})"
            else -> DiagnosisOutcome.DIAGNOSE to "ACCEPTED"
        }

        return when (outcome) {
            DiagnosisOutcome.CANT_TELL -> {
                DiagnosisEvaluation(
                    classId = "unknown",
                    displayName = "Not sure - ask an extension officer",
                    confidence = top1Prob,
                    certaintyBand = CertaintyBand.NOT_SURE,
                    top3 = topK,
                    freeEnergy = freeEnergy,
                    isOutOfDistribution = isOOD,
                    isBelowThreshold = true,
                    outcome = outcome,
                    outcomeReason = outcomeReason,
                    margin = margin
                )
            }
            DiagnosisOutcome.OUT_OF_CLASS -> {
                val resolvedClassId = if (isUnsupportedClass) "unknown" else "out_of_class"
                val resolvedDisplayName = if (isUnsupportedClass) "Not sure - ask an extension officer" else "Out of class - plant does not match"
                DiagnosisEvaluation(
                    classId = resolvedClassId,
                    displayName = resolvedDisplayName,
                    confidence = top1Prob,
                    certaintyBand = CertaintyBand.NOT_SURE,
                    top3 = topK,
                    freeEnergy = freeEnergy,
                    isOutOfDistribution = isOOD,
                    isBelowThreshold = true,
                    outcome = outcome,
                    outcomeReason = outcomeReason,
                    margin = margin
                )
            }
            DiagnosisOutcome.DIAGNOSE -> {
                DiagnosisEvaluation(
                    classId = top1!!.classId,
                    displayName = top1.displayName,
                    confidence = top1Prob,
                    certaintyBand = CertaintyCalculator.calculate(top1Prob),
                    top3 = topK,
                    freeEnergy = freeEnergy,
                    isOutOfDistribution = false,
                    isBelowThreshold = false,
                    outcome = outcome,
                    outcomeReason = outcomeReason,
                    margin = margin
                )
            }
        }
    }

    /**
     * Task AUTO: Automatic crop detection decision evaluation without pre-selected crop.
     * Enforces fail-closed rules from Requirement 2:
     * CAN'T TELL if:
     * - Top class is not a crop (weeds_not_a_crop or other_not_a_crop or crop is null)
     * - Energy is above threshold
     * - Top crop's mass is below threshold
     * - Top two classes belong to different crops with margin below threshold
     * - Confidence below threshold
     * - Leaf validator fails
     */
    fun evaluateDiagnosisAuto(
        labels: List<String>,
        probabilities: FloatArray,
        thresholds: Map<String, Float> = emptyMap(),
        defaultThreshold: Float = 0.40f,
        freeEnergy: Double = 0.0,
        energyThreshold: Double? = null,
        cropMassThreshold: Float = 0.40f,
        marginThreshold: Float = 0.15f,
        gate1Pass: Boolean = true,
        leafValidatorReason: String = ""
    ): DiagnosisEvaluation {
        val topK = extractTopK(labels, probabilities, 3)
        val top1 = topK.firstOrNull()
        val top2 = if (topK.size > 1) topK[1] else null
        val top1Prob = top1?.probability ?: 0.0f
        val top2Prob = top2?.probability ?: 0.0f
        val margin = top1Prob - top2Prob

        val top1Class = top1?.classId ?: "unknown"
        val top1Crop = com.cropdoctor.domain.CropMapping.getCropForClass(top1Class)
        val top2Class = top2?.classId ?: "unknown"
        val top2Crop = com.cropdoctor.domain.CropMapping.getCropForClass(top2Class)

        val isNotACrop = top1Class == "weeds_not_a_crop" || top1Class == "other_not_a_crop" || top1Crop == null
        val isOOD = if (energyThreshold != null) freeEnergy > energyThreshold else false

        val requiredThreshold = thresholds[top1Class] ?: defaultThreshold
        val isBelowThreshold = top1 == null || top1Prob < requiredThreshold

        val cropMasses = com.cropdoctor.domain.CropMapping.computeCropMasses(labels, probabilities)
        val topCropMass = if (top1Crop != null) cropMasses[top1Crop] ?: 0.0f else 0.0f

        val diffCrops = (top1Crop != null && top2Crop != null && top1Crop != top2Crop)
        val isCrossCropMarginViolated = diffCrops && margin < marginThreshold

        val (outcome, outcomeReason) = when {
            !gate1Pass -> DiagnosisOutcome.CANT_TELL to "LEAF_VALIDATOR_FAIL($leafValidatorReason)"
            isNotACrop -> DiagnosisOutcome.CANT_TELL to "NOT_A_CROP($top1Class)"
            isOOD -> DiagnosisOutcome.CANT_TELL to "HIGH_FREE_ENERGY($freeEnergy > $energyThreshold)"
            isBelowThreshold -> DiagnosisOutcome.CANT_TELL to "LOW_CONFIDENCE($top1Prob < $requiredThreshold)"
            topCropMass < cropMassThreshold -> DiagnosisOutcome.CANT_TELL to "LOW_CROP_MASS($topCropMass < $cropMassThreshold)"
            isCrossCropMarginViolated -> DiagnosisOutcome.CANT_TELL to "CROSS_CROP_MARGIN($margin < $marginThreshold)"
            else -> DiagnosisOutcome.DIAGNOSE to "ACCEPTED"
        }

        return when (outcome) {
            DiagnosisOutcome.CANT_TELL -> {
                DiagnosisEvaluation(
                    classId = "unknown",
                    displayName = "Not sure - ask an extension officer",
                    confidence = top1Prob,
                    certaintyBand = CertaintyBand.NOT_SURE,
                    top3 = topK,
                    freeEnergy = freeEnergy,
                    isOutOfDistribution = isOOD,
                    isBelowThreshold = true,
                    outcome = outcome,
                    outcomeReason = outcomeReason,
                    margin = margin
                )
            }
            DiagnosisOutcome.OUT_OF_CLASS -> {
                DiagnosisEvaluation(
                    classId = "out_of_class",
                    displayName = "Out of class - plant does not match",
                    confidence = top1Prob,
                    certaintyBand = CertaintyBand.NOT_SURE,
                    top3 = topK,
                    freeEnergy = freeEnergy,
                    isOutOfDistribution = isOOD,
                    isBelowThreshold = true,
                    outcome = outcome,
                    outcomeReason = outcomeReason,
                    margin = margin
                )
            }
            DiagnosisOutcome.DIAGNOSE -> {
                DiagnosisEvaluation(
                    classId = top1!!.classId,
                    displayName = top1.displayName,
                    confidence = top1Prob,
                    certaintyBand = CertaintyCalculator.calculate(top1Prob),
                    top3 = topK,
                    freeEnergy = freeEnergy,
                    isOutOfDistribution = false,
                    isBelowThreshold = false,
                    outcome = outcome,
                    outcomeReason = outcomeReason,
                    margin = margin
                )
            }
        }
    }
}
