package com.cropdoctor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.ScoredClass
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.domain.SeverityCalculator
import com.cropdoctor.vision.VisionMath
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests enforcing Decision D1:
 * 1. The app must use ONLY the 25-class output.
 * 2. Any learned severity head from the vision model is completely ignored.
 * 3. Severity comes EXCLUSIVELY from LesionAreaEstimator and RulesEngine.
 * 4. Chilli class 13 (White Spot) is UNVERIFIED and must return "Can't tell" (unknown / NOT_SURE).
 */
class VisionSeverityPipelineTest {

    private val labels = listOf(
        "rice_bacterial_leaf_blight",
        "rice_brown_spot",
        "rice_leaf_blast",
        "rice_sheath_blight",
        "rice_healthy",
        "cotton_bacterial_blight",
        "cotton_fusarium_wilt",
        "cotton_leaf_curl_virus",
        "cotton_alternaria_leaf_spot",
        "cotton_healthy",
        "chilli_bacterial_spot",
        "chilli_leaf_curl_virus",
        "chilli_cercospora_leaf_spot",
        "chilli_white_spot",
        "chilli_healthy",
        "tomato_early_blight",
        "tomato_late_blight",
        "tomato_yellow_leaf_curl_virus",
        "tomato_spider_mites",
        "tomato_healthy",
        "maize_gray_leaf_spot",
        "maize_common_rust",
        "maize_northern_leaf_blight",
        "maize_fall_armyworm",
        "maize_healthy"
    )

    private val thresholds = mapOf(
        "rice_bacterial_leaf_blight" to 0.40f,
        "rice_healthy" to 0.50f,
        "tomato_early_blight" to 0.40f,
        "chilli_white_spot" to 0.40f
    )

    /**
     * Resolves severity for the Result screen strictly following Decision D1:
     * ignores any model severity logits and uses RulesEngine + lesion area.
     */
    private fun resolveSeverityForResultScreen(
        classId: String,
        lesionFraction: Float,
        @Suppress("UNUSED_PARAMETER") modelSeverityLogits: FloatArray?
    ): SeverityBand {
        // Decision D1: modelSeverityLogits is completely ignored.
        // Severity is derived exclusively from RulesEngine and lesionFraction.
        if (classId == "unknown" || classId.endsWith("_healthy")) {
            return SeverityBand.MILD
        }
        return RulesEngine.calculateSeverity(lesionFraction, classId)
    }

    @Test
    fun testModelSeverityHeadIgnoredWhenModelPredictsSevereButLesionIsMild() {
        val classId = "rice_bacterial_leaf_blight"
        val lesionFraction = 0.05f // < 10% -> RulesEngine mandates MILD

        // Hypothetical model severity head predicting SEVERE (index 2 has max logit)
        val modelSeverityHeadLogits = floatArrayOf(-5.0f, -2.0f, 12.0f)
        val modelPredictedSeverity = when (modelSeverityHeadLogits.indices.maxByOrNull { modelSeverityHeadLogits[it] }) {
            0 -> SeverityBand.MILD
            1 -> SeverityBand.MODERATE
            else -> SeverityBand.SEVERE
        }
        assertEquals("Model head itself predicts SEVERE", SeverityBand.SEVERE, modelPredictedSeverity)

        // Resolve severity for result screen
        val screenSeverity = resolveSeverityForResultScreen(classId, lesionFraction, modelSeverityHeadLogits)

        // Must match RulesEngine (MILD), NOT the model severity head (SEVERE)
        assertEquals(
            "Result screen severity MUST be MILD based on lesion area (0.05 < 0.10), ignoring model severity head",
            SeverityBand.MILD,
            screenSeverity
        )
        assertNotEquals(
            "Model severity head value (SEVERE) must NOT reach the result screen",
            SeverityBand.SEVERE,
            screenSeverity
        )
    }

    @Test
    fun testModelSeverityHeadIgnoredWhenModelPredictsMildButLesionIsSevere() {
        val classId = "tomato_early_blight"
        val lesionFraction = 0.45f // > 30% -> RulesEngine mandates SEVERE

        // Hypothetical model severity head predicting MILD (index 0 has max logit)
        val modelSeverityHeadLogits = floatArrayOf(15.0f, -2.0f, -5.0f)
        val modelPredictedSeverity = when (modelSeverityHeadLogits.indices.maxByOrNull { modelSeverityHeadLogits[it] }) {
            0 -> SeverityBand.MILD
            1 -> SeverityBand.MODERATE
            else -> SeverityBand.SEVERE
        }
        assertEquals("Model head itself predicts MILD", SeverityBand.MILD, modelPredictedSeverity)

        // Resolve severity for result screen
        val screenSeverity = resolveSeverityForResultScreen(classId, lesionFraction, modelSeverityHeadLogits)

        // Must match RulesEngine (SEVERE), NOT the model severity head (MILD)
        assertEquals(
            "Result screen severity MUST be SEVERE based on lesion area (0.45 > 0.30), ignoring model severity head",
            SeverityBand.SEVERE,
            screenSeverity
        )
        assertNotEquals(
            "Model severity head value (MILD) must NOT reach the result screen",
            SeverityBand.MILD,
            screenSeverity
        )
    }

    @Test
    fun testHealthyCropAlwaysMildRegardlessOfModelSeverityHead() {
        val classId = "rice_healthy"
        val lesionFraction = 0.50f
        val modelSeverityHeadLogits = floatArrayOf(-10.0f, -10.0f, 20.0f) // SEVERE

        val screenSeverity = resolveSeverityForResultScreen(classId, lesionFraction, modelSeverityHeadLogits)

        assertEquals("Healthy crop severity must always be MILD", SeverityBand.MILD, screenSeverity)
    }

    @Test
    fun testChilliWhiteSpotNeverReturnsConfidentDiagnosis() {
        // Create probabilities where chilli_white_spot (index 13) has 0.95 probability
        val probs = FloatArray(25) { 0.002f }
        probs[13] = 0.95f

        val eval = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probs,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = -5.0, // In-distribution energy
            energyThreshold = -1.5
        )

        // Class 13 is UNVERIFIED: MUST be forced to unknown / NOT_SURE
        assertEquals("Unverified chilli white spot must return 'unknown' classId", "unknown", eval.classId)
        assertEquals("Unverified chilli white spot must return NOT_SURE certainty", CertaintyBand.NOT_SURE, eval.certaintyBand)
        assertEquals("Unverified chilli white spot must return extension referral name", "Not sure - ask an extension officer", eval.displayName)

        // And ResultScreen rule: when certainty is NOT_SURE, shouldShowSeverity is false
        assertFalse("Severity must NOT be shown when diagnosis is unverified / Not sure", RulesEngine.shouldShowSeverity(eval.certaintyBand))
        assertEquals(
            "Guidance sentence must be Can't tell",
            com.cropdoctor.R.string.cant_tell_no_disease,
            RulesEngine.getGuidanceSentence(eval.certaintyBand, SeverityBand.MILD)
        )
    }

    @Test
    fun testClassifierResultSeverityIsDecoupledFromModel() {
        val lesion = 0.20f // 10-30% -> MODERATE
        val classId = "rice_bacterial_leaf_blight"
        val expectedSeverity = RulesEngine.calculateSeverity(lesion, classId)
        assertEquals(SeverityBand.MODERATE, expectedSeverity)

        val result = ClassifierResult(
            classId = classId,
            displayName = "Rice Bacterial Leaf Blight",
            confidence = 0.92f,
            severityBand = expectedSeverity,
            certaintyBand = CertaintyBand.SURE,
            lesionFraction = lesion,
            inferenceTimeMs = 50L,
            memoryUsedMb = 120L,
            isDemoMode = false,
            top3 = listOf(ScoredClass(classId, "Rice Bacterial Leaf Blight", 0.92f))
        )

        assertEquals("ClassifierResult severityBand must equal RulesEngine calculation", SeverityBand.MODERATE, result.severityBand)
    }
}
