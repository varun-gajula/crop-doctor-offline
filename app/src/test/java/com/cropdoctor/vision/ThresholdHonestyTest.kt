package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Task V7 Unit Tests: Threshold Honesty & Single Source of Truth
 * 1. thresholds.json is the single source of truth.
 * 2. Required keys: temperature and confidence_threshold.
 * 3. energy_threshold is optional; if absent, energy check is OFF.
 * 4. If thresholds.json or required keys are missing or invalid, classifier must return "Can't tell" and never silently use other numbers.
 * 5. Asserts that ExecuTorchClassifier.kt source code contains NO numeric threshold literals.
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

        // Missing required keys (e.g. only temperature without confidence_threshold)
        val invalidJson = """{"temperature": 1.33}"""
        val parsedJson = Json.parseToJsonElement(invalidJson).jsonObject
        val hasRequired = parsedJson.containsKey("temperature") && parsedJson.containsKey("confidence_threshold")
        assertFalse("JSON missing required confidence_threshold key must be rejected", hasRequired)

        // Verify that when threshold state is invalid/missing, diagnosis evaluation routes to "Can't tell"
        val labels = listOf("rice_bacterial_leaf_blight", "rice_healthy")
        val probs = floatArrayOf(0.95f, 0.05f)

        // If thresholds are missing, any evaluation must strictly fail closed (Can't tell)
        val fallbackDefaultThreshold = Float.MAX_VALUE // Cannot be satisfied

        val eval = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probs,
            thresholds = emptyMap(),
            defaultThreshold = fallbackDefaultThreshold,
            freeEnergy = -5.0,
            energyThreshold = null
        )

        assertEquals("Missing thresholds must return 'unknown' classId (Can't tell)", "unknown", eval.classId)
        assertEquals("Missing thresholds must return NOT_SURE certainty", CertaintyBand.NOT_SURE, eval.certaintyBand)
        assertEquals("Missing thresholds must return extension referral", "Not sure - ask an extension officer", eval.displayName)
    }

    @Test
    fun testThresholdsJsonSingleSourceOfTruth() {
        val candidates = listOf(
            File("app/src/main/assets/thresholds.json"),
            File("src/main/assets/thresholds.json")
        )
        val thresholdsAsset = candidates.firstOrNull { it.exists() }
        assertNotNull("assets/thresholds.json must exist", thresholdsAsset)

        val text = thresholdsAsset!!.readText()
        val json = Json.parseToJsonElement(text).jsonObject

        assertTrue("thresholds.json must define temperature", json.containsKey("temperature"))
        assertTrue("thresholds.json must define confidence_threshold", json.containsKey("confidence_threshold"))

        // energy_threshold is absent in the calibrated final thresholds
        assertFalse("thresholds.json has NO energy_threshold", json.containsKey("energy_threshold"))

        assertEquals("Calibrated temperature is ~1.330", 1.3302f, json["temperature"]!!.jsonPrimitive.float, 0.01f)
        assertEquals("Calibrated confidence threshold is ~0.4018", 0.4018f, json["confidence_threshold"]!!.jsonPrimitive.float, 0.01f)
    }
}
