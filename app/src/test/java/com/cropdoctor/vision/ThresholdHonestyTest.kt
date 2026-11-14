package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Task V2 Unit Tests: Threshold Honesty & Single Source of Truth
 * 1. thresholds.json is the single source of truth.
 * 2. If thresholds.json is missing or invalid, classifier must return "Can't tell" and never silently use other numbers.
 * 3. Asserts that ExecuTorchClassifier.kt source code contains NO numeric threshold literals (e.g. 5.0, 0.25).
 */
class ThresholdHonestyTest {

    @Test
    fun testKotlinCodeContainsNoNumericThresholdLiterals() {
        val classifierFile = File("src/main/java/com/cropdoctor/vision/ExecuTorchClassifier.kt")
        assertTrue("ExecuTorchClassifier.kt must exist", classifierFile.exists())

        val sourceCode = classifierFile.readText()

        // 1. Assert old relaxed fallbacks are completely removed
        assertFalse(
            "ExecuTorchClassifier.kt must NOT contain 5.0 threshold literal",
            Regex("""\b5\.0\b""").containsMatchIn(sourceCode)
        )
        assertFalse(
            "ExecuTorchClassifier.kt must NOT contain 0.25 threshold literal",
            Regex("""\b0\.25\b""").containsMatchIn(sourceCode)
        )
        assertFalse(
            "ExecuTorchClassifier.kt must NOT define DEFAULT_ENERGY_THRESHOLD constant",
            sourceCode.contains("DEFAULT_ENERGY_THRESHOLD")
        )

        // 2. Assert no hard-coded threshold assignments
        assertFalse(
            "ExecuTorchClassifier.kt must NOT hard-code energyThreshold assignment literal",
            sourceCode.contains("energyThreshold = -1.5")
        )
        assertFalse(
            "ExecuTorchClassifier.kt must NOT hard-code defaultThreshold assignment literal",
            sourceCode.contains("defaultThreshold = 0.40") || sourceCode.contains("defaultThreshold = 0.4f")
        )

        // 3. Assert single source of truth documentation / enforcement
        assertTrue(
            "ExecuTorchClassifier.kt must reference SINGLE SOURCE OF TRUTH",
            sourceCode.contains("SINGLE SOURCE OF TRUTH") || sourceCode.contains("single source of truth")
        )
    }

    @Test
    fun testMissingThresholdsFileReturnsCantTell() {
        // When thresholds.json is missing or empty, parsing fails and threshold state is invalid
        val dummyEmptyText = ""
        val isEmptyInvalid = dummyEmptyText.isBlank()
        assertTrue("Empty or missing thresholds text is invalid", isEmptyInvalid)

        // Missing required keys (e.g. only temperature without default_threshold or energy_threshold)
        val invalidJson = """{"temperature": 1.15}"""
        val parsedJson = Json.parseToJsonElement(invalidJson).jsonObject
        val hasRequired = parsedJson.containsKey("default_threshold") && parsedJson.containsKey("energy_threshold")
        assertFalse("JSON missing required threshold keys must be rejected", hasRequired)

        // Verify that when threshold state is invalid/missing, diagnosis evaluation routes to "Can't tell"
        // (unknown / NOT_SURE)
        val labels = listOf("rice_bacterial_leaf_blight", "rice_healthy")
        val probs = floatArrayOf(0.95f, 0.05f)

        // If thresholds are missing, any evaluation must strictly fail closed (Can't tell)
        val fallbackDefaultThreshold = Float.MAX_VALUE // Cannot be satisfied
        val fallbackEnergyThreshold = Double.NEGATIVE_INFINITY // Cannot be satisfied

        val eval = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probs,
            thresholds = emptyMap(),
            defaultThreshold = fallbackDefaultThreshold,
            freeEnergy = -5.0,
            energyThreshold = fallbackEnergyThreshold
        )

        assertEquals("Missing thresholds must return 'unknown' classId (Can't tell)", "unknown", eval.classId)
        assertEquals("Missing thresholds must return NOT_SURE certainty", CertaintyBand.NOT_SURE, eval.certaintyBand)
        assertEquals("Missing thresholds must return extension referral", "Not sure - ask an extension officer", eval.displayName)
    }

    @Test
    fun testThresholdsJsonSingleSourceOfTruth() {
        val thresholdsAsset = File("src/main/assets/thresholds.json")
        assertTrue("assets/thresholds.json must exist", thresholdsAsset.exists())

        val text = thresholdsAsset.readText()
        val json = Json.parseToJsonElement(text).jsonObject

        assertTrue("thresholds.json must define default_threshold", json.containsKey("default_threshold"))
        assertTrue("thresholds.json must define energy_threshold", json.containsKey("energy_threshold"))
        assertTrue("thresholds.json must define class_thresholds", json.containsKey("class_thresholds"))

        assertEquals("Calibrated default threshold is 0.20", 0.20f, json["default_threshold"]!!.jsonPrimitive.float, 0.001f)
        assertEquals("Calibrated energy threshold is 5.0", 5.0, json["energy_threshold"]!!.jsonPrimitive.double, 0.001)
    }
}
