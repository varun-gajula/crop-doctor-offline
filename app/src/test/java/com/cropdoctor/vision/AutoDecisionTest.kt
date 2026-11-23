package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.CropMapping
import com.cropdoctor.domain.DiagnosisOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Task AUTO: Unit tests for automatic crop detection decision rules,
 * fail-closed gates, thresholds, and outcome handling.
 */
class AutoDecisionTest {

    private val labels = listOf(
        "rice_leaf_blast",
        "rice_healthy",
        "cotton_bacterial_blight",
        "cotton_healthy",
        "chilli_leaf_curl_virus",
        "chilli_healthy",
        "maize_northern_leaf_blight",
        "maize_common_rust",
        "maize_healthy",
        "maize_fall_armyworm",
        "rice_stem_borer",
        "rice_hispa",
        "tomato_healthy",
        "tomato_septoria_leaf_spot",
        "other_not_a_crop"
    )

    private val defaultThreshold = 0.3434087f
    private val energyThreshold = -8.0
    private val cropMassThreshold = 0.60f
    private val marginThreshold = 0.25f

    @Test
    fun testThresholdsJsonKeyPresenceAndValues() {
        val thresholdsFile = File("src/main/assets/thresholds.json")
        assertTrue("thresholds.json must exist in assets", thresholdsFile.exists())
        val json = Json.parseToJsonElement(thresholdsFile.readText()).jsonObject

        assertTrue("Must contain default_threshold or confidence_threshold",
            json.containsKey("default_threshold") || json.containsKey("confidence_threshold"))
        assertTrue("Must contain energy_threshold", json.containsKey("energy_threshold"))

        val defThresh = (json["default_threshold"] ?: json["confidence_threshold"])?.jsonPrimitive?.double ?: 0.0
        val eThresh = json["energy_threshold"]?.jsonPrimitive?.double ?: 0.0

        assertTrue("Confidence threshold should be between 0.2 and 0.8", defThresh in 0.2..0.8)
        assertTrue("Energy threshold should be negative", eThresh < 0.0)
    }

    @Test
    fun testGate1LeafValidatorFailureGivesCantTell() {
        val probs = FloatArray(labels.size) { 0.01f }
        probs[0] = 0.80f // High rice confidence

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            gate1Pass = false,
            leafValidatorReason = "Not enough plant green"
        )

        assertEquals(DiagnosisOutcome.CANT_TELL, eval.outcome)
        assertEquals("unknown", eval.classId)
        assertTrue(eval.outcomeReason.startsWith("LEAF_VALIDATOR_FAIL"))
    }

    @Test
    fun testWeedsNotACropGivesCantTell() {
        val probs = FloatArray(labels.size) { 0.01f }
        val weedsIdx = labels.indexOf("other_not_a_crop")
        probs[weedsIdx] = 0.85f

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            gate1Pass = true
        )

        assertEquals(DiagnosisOutcome.CANT_TELL, eval.outcome)
        assertEquals("unknown", eval.classId)
        assertTrue(eval.outcomeReason.startsWith("NOT_A_CROP"))
    }

    @Test
    fun testHighFreeEnergyGivesCantTell() {
        val probs = FloatArray(labels.size) { 0.01f }
        probs[0] = 0.80f

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -5.0, // -5.0 > -8.0 -> OOD
            energyThreshold = energyThreshold,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            gate1Pass = true
        )

        assertEquals(DiagnosisOutcome.CANT_TELL, eval.outcome)
        assertEquals("unknown", eval.classId)
        assertTrue(eval.outcomeReason.startsWith("HIGH_FREE_ENERGY"))
    }

    @Test
    fun testLowConfidenceBelowThresholdGivesCantTell() {
        val probs = FloatArray(labels.size) { 0.04f }
        probs[0] = 0.20f // 0.20 < 0.3434

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMassThreshold = 0.10f, // lower crop mass to isolate confidence check
            marginThreshold = 0.05f,
            gate1Pass = true
        )

        assertEquals(DiagnosisOutcome.CANT_TELL, eval.outcome)
        assertEquals("unknown", eval.classId)
        assertTrue(eval.outcomeReason.startsWith("LOW_CONFIDENCE"))
    }

    @Test
    fun testLowTopCropMassGivesCantTell() {
        val probs = FloatArray(labels.size) { 0.0f }
        // Rice top class is 0.45, but rest of mass is scattered across other crops
        probs[0] = 0.45f // rice_leaf_blast
        probs[2] = 0.25f // cotton_bacterial_blight
        probs[4] = 0.15f // chilli_leaf_curl_virus
        probs[13] = 0.15f // tomato_septoria_leaf_spot
        // Top crop mass (rice) = 0.45 < 0.60 threshold

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = 0.10f,
            gate1Pass = true
        )

        assertEquals(DiagnosisOutcome.CANT_TELL, eval.outcome)
        assertEquals("unknown", eval.classId)
        assertTrue(eval.outcomeReason.startsWith("LOW_CROP_MASS"))
    }

    @Test
    fun testCrossCropMarginViolationGivesCantTell() {
        val probs = FloatArray(labels.size) { 0.0f }
        // Top 1: rice_leaf_blast (0.40)
        // Top 2: cotton_bacterial_blight (0.35)
        // Different crops! Margin = 0.40 - 0.35 = 0.05 < marginThreshold (0.25)
        probs[0] = 0.40f // rice_leaf_blast
        probs[2] = 0.35f // cotton_bacterial_blight
        probs[1] = 0.25f // rice_healthy (total rice mass = 0.65 >= 0.60)

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            gate1Pass = true
        )

        assertEquals(DiagnosisOutcome.CANT_TELL, eval.outcome)
        assertEquals("unknown", eval.classId)
        assertTrue(eval.outcomeReason.startsWith("CROSS_CROP_MARGIN"))
    }

    @Test
    fun testSameCropCloseMarginDoesNotFailCrossCropGate() {
        val probs = FloatArray(labels.size) { 0.0f }
        // Top 1: rice_leaf_blast (0.45)
        // Top 2: rice_healthy (0.40)
        // SAME CROP (rice)! Margin = 0.05, but diffCrops is false!
        probs[0] = 0.45f // rice_leaf_blast
        probs[1] = 0.40f // rice_healthy
        probs[10] = 0.15f // rice_stem_borer (total rice mass = 1.00)

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            gate1Pass = true
        )

        assertEquals(DiagnosisOutcome.DIAGNOSE, eval.outcome)
        assertEquals("rice_leaf_blast", eval.classId)
        assertEquals("ACCEPTED", eval.outcomeReason)
    }

    @Test
    fun testConfidentSingleCropDiagnosisAccepted() {
        val probs = FloatArray(labels.size) { 0.005f }
        probs[4] = 0.85f // chilli_leaf_curl_virus

        val eval = VisionMath.evaluateDiagnosisAuto(
            labels = labels,
            probabilities = probs,
            defaultThreshold = defaultThreshold,
            freeEnergy = -15.0,
            energyThreshold = energyThreshold,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            gate1Pass = true
        )

        assertEquals(DiagnosisOutcome.DIAGNOSE, eval.outcome)
        assertEquals("chilli_leaf_curl_virus", eval.classId)
        assertEquals("ACCEPTED", eval.outcomeReason)
        assertEquals(CertaintyBand.SURE, eval.certaintyBand)
    }

    @Test
    fun testCropMassComputation() {
        val probs = FloatArray(labels.size) { 0.0f }
        probs[0] = 0.3f // rice_leaf_blast
        probs[1] = 0.2f // rice_healthy
        probs[2] = 0.5f // cotton_bacterial_blight

        val masses = CropMapping.computeCropMasses(labels, probs)
        assertEquals(0.5f, masses["rice"] ?: 0f, 0.001f)
        assertEquals(0.5f, masses["cotton"] ?: 0f, 0.001f)
        assertEquals(0.0f, masses["chilli"] ?: 0f, 0.001f)
    }

    @Test
    fun testDynamicCropPrefixExtractionWorksForAnyLabels() {
        val customLabels = listOf(
            "banana_leaf_spot",
            "wheat_rust",
            "sugarcane_smut",
            "weeds_not_a_crop",
            "other_not_a_crop"
        )
        val customProbs = floatArrayOf(0.7f, 0.2f, 0.05f, 0.03f, 0.02f)

        val masses = CropMapping.computeCropMasses(customLabels, customProbs)
        assertEquals(0.7f, masses["banana"] ?: 0f, 0.001f)
        assertEquals(0.2f, masses["wheat"] ?: 0f, 0.001f)
        assertEquals(0.05f, masses["sugarcane"] ?: 0f, 0.001f)
        assertFalse(masses.containsKey("weeds"))
        assertFalse(masses.containsKey("other"))
    }

    @Test
    fun testPickerMaskOverrideRerunsWithP3P4Logic() {
        val logits = FloatArray(labels.size) { -10.0f }
        logits[0] = 2.0f // rice_leaf_blast
        logits[2] = 1.8f // cotton_bacterial_blight
        logits[1] = 1.0f // rice_healthy
        logits[10] = 0.0f // rice_stem_borer

        // When the farmer selects "cotton" as mask:
        val cottonMask = VisionMath.maskAndRenormalizeWithMass(labels, logits, "cotton")
        val evalCotton = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = cottonMask.probabilities,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMass = cottonMask.cropMass,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            borderlineEnergy = -9.0
        )

        // Cotton mass in this distribution is only ~0.37 < 0.60 threshold -> OUT_OF_CLASS
        assertEquals(DiagnosisOutcome.OUT_OF_CLASS, evalCotton.outcome)

        // When the farmer selects "rice" as mask:
        val riceMask = VisionMath.maskAndRenormalizeWithMass(labels, logits, "rice")
        val evalRice = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = riceMask.probabilities,
            defaultThreshold = defaultThreshold,
            freeEnergy = -12.0,
            energyThreshold = energyThreshold,
            cropMass = riceMask.cropMass,
            cropMassThreshold = cropMassThreshold,
            marginThreshold = marginThreshold,
            borderlineEnergy = -9.0
        )

        // Rice mass is ~0.63 >= 0.60 -> DIAGNOSE rice_leaf_blast
        assertEquals(DiagnosisOutcome.DIAGNOSE, evalRice.outcome)
        assertEquals("rice_leaf_blast", evalRice.classId)
    }
}
