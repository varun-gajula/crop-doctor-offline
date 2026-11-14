package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.CertaintyCalculator
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
    val isBelowThreshold: Boolean
)

object VisionMath {

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

    /**
     * Crop chip masking: when a crop is chosen, keep only that crop's classes and re-normalise.
     * "Auto" / "all" / null uses all 25 classes.
     */
    fun maskAndRenormalize(
        labels: List<String>,
        scaledLogits: FloatArray,
        cropHint: String?
    ): FloatArray {
        val n = labels.size
        val probabilities = FloatArray(n) { 0.0f }
        if (n == 0 || scaledLogits.isEmpty()) return probabilities

        val normalizedHint = cropHint?.lowercase()?.trim()
        val isAuto = normalizedHint == null || normalizedHint == "all" || normalizedHint == "auto" || normalizedHint.isEmpty()

        val matchingIndices = if (isAuto) {
            labels.indices.toList()
        } else {
            val prefix = "${normalizedHint}_"
            labels.indices.filter { labels[it].startsWith(prefix) }
        }

        val targetIndices = if (matchingIndices.isNotEmpty()) matchingIndices else labels.indices.toList()

        var maxVal = Float.NEGATIVE_INFINITY
        for (idx in targetIndices) {
            if (idx < scaledLogits.size && scaledLogits[idx] > maxVal) {
                maxVal = scaledLogits[idx]
            }
        }

        var sum = 0.0f
        for (idx in targetIndices) {
            if (idx < scaledLogits.size) {
                val expVal = exp(scaledLogits[idx] - maxVal)
                probabilities[idx] = expVal
                sum += expVal
            }
        }

        if (sum > 0.0f) {
            for (idx in targetIndices) {
                probabilities[idx] /= sum
            }
        }

        return probabilities
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
     * Evaluates diagnosis with temperature scaling, confidence thresholds, and OOD detection.
     */
    fun evaluateDiagnosis(
        labels: List<String>,
        probabilities: FloatArray,
        thresholds: Map<String, Float>,
        defaultThreshold: Float,
        freeEnergy: Double,
        energyThreshold: Double
    ): DiagnosisEvaluation {
        val topK = extractTopK(labels, probabilities, 3)
        val top1 = topK.firstOrNull()

        // Task V1 / seed/class_names_checked.tsv row 15:
        // Chilli class 13 ("White Spot" / "chilli_white_spot") has UNVERIFIED cause.
        // It must never be returned as a confident diagnosis; returns "unknown" ("Can't tell").
        val isUnverifiedClass = top1?.classId == "chilli_white_spot"

        val isOOD = freeEnergy > energyThreshold
        val requiredThreshold = if (top1 != null) {
            thresholds[top1.classId] ?: defaultThreshold
        } else {
            defaultThreshold
        }

        val isBelowThreshold = top1 == null || top1.probability < requiredThreshold

        return if (isOOD || isBelowThreshold || isUnverifiedClass) {
            val candidateConfidence = top1?.probability ?: 0.0f
            DiagnosisEvaluation(
                classId = "unknown",
                displayName = "Not sure - ask an extension officer",
                confidence = candidateConfidence,
                certaintyBand = CertaintyBand.NOT_SURE,
                top3 = topK,
                freeEnergy = freeEnergy,
                isOutOfDistribution = isOOD,
                isBelowThreshold = isBelowThreshold || isUnverifiedClass
            )
        } else {
            DiagnosisEvaluation(
                classId = top1.classId,
                displayName = top1.displayName,
                confidence = top1.probability,
                certaintyBand = CertaintyCalculator.calculate(top1.probability),
                top3 = topK,
                freeEnergy = freeEnergy,
                isOutOfDistribution = false,
                isBelowThreshold = false
            )
        }
    }
}

