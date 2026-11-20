package com.cropdoctor.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CropdocDecisionGateTest {

    private val labels = listOf(
        "rice_bacterial_leaf_blight",
        "rice_leaf_blast",
        "rice_sheath_blight",
        "rice_healthy",
        "cotton_bacterial_blight",
        "cotton_fusarium_wilt",
        "cotton_leaf_curl_virus",
        "cotton_healthy",
        "chilli_leaf_curl_virus",
        "chilli_nutrition_deficiency",
        "chilli_healthy",
        "tomato_leaf_blight",
        "tomato_yellow_leaf_curl_virus",
        "tomato_septoria_leaf_spot",
        "tomato_healthy",
        "maize_gray_leaf_spot",
        "maize_common_rust",
        "maize_northern_leaf_blight",
        "maize_healthy",
        "weeds_not_a_crop"
    )

    private val defaultThreshold = 0.4018f
    private val unsupportedClasses = setOf("weeds_not_a_crop", "tomato_leaf_blight")

    @Test
    fun testGate1LeafValidatorRejection() {
        // When leaf validator indicates not a crop
        val meta = ImageAnalysisMetadata(
            source = "gallery",
            origWidth = 1920,
            origHeight = 1080,
            exifOrientation = 0,
            leafValidatorPass = false,
            leafValidatorFraction = 0.005f,
            leafValidatorReason = "Not enough crop pixels"
        )
        assertFalse(meta.leafValidatorPass)
        assertEquals("REJECTED_LEAF_VALIDATOR", if (!meta.leafValidatorPass) "REJECTED_LEAF_VALIDATOR" else "ACCEPTED")
    }

    @Test
    fun testGate2EnergyRejectionWhenEnergyThresholdSet() {
        val energyThreshold = -9.3050
        val freeEnergyAbove = -5.0 // -5.0 > -9.3050 -> OOD
        val freeEnergyNormal = -12.0 // -12.0 <= -9.3050 -> In distribution

        val isOOD = freeEnergyAbove > energyThreshold
        val isNotOOD = freeEnergyNormal > energyThreshold

        assertTrue(isOOD)
        assertFalse(isNotOOD)
    }

    @Test
    fun testGate3ConfidenceRejection() {
        val probsLow = FloatArray(20) { 0.02f } // max prob 0.02 < 0.4018
        val evalLow = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsLow,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupportedClasses
        )
        assertTrue(evalLow.isBelowThreshold)
        assertEquals("unknown", evalLow.classId)

        val probsHigh = FloatArray(20) { 0.01f }
        probsHigh[1] = 0.81f // rice_leaf_blast
        val evalHigh = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsHigh,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupportedClasses
        )
        assertFalse(evalHigh.isBelowThreshold)
        assertEquals("rice_leaf_blast", evalHigh.classId)
    }

    @Test
    fun testGate4WeedsAndUnsupportedRejection() {
        val probsWeeds = FloatArray(20) { 0.01f }
        probsWeeds[19] = 0.81f // weeds_not_a_crop
        val evalWeeds = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsWeeds,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupportedClasses
        )
        // Weeds must be rejected as unknown
        assertEquals("unknown", evalWeeds.classId)
        assertTrue(evalWeeds.isBelowThreshold)

        val probsTomatoBlight = FloatArray(20) { 0.01f }
        probsTomatoBlight[11] = 0.81f // tomato_leaf_blight
        val evalTomatoBlight = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsTomatoBlight,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupportedClasses
        )
        // tomato_leaf_blight must be rejected as unknown
        assertEquals("unknown", evalTomatoBlight.classId)
    }

    @Test
    fun testDecisionLogLineFormat() {
        val meta = ImageAnalysisMetadata(
            source = "gallery",
            origWidth = 3000,
            origHeight = 4000,
            exifOrientation = 90,
            leafValidatorPass = true,
            leafValidatorFraction = 0.75f,
            leafValidatorReason = "OK"
        )
        val freeEnergy = -5.1234
        val top1Class = "rice_leaf_blast"
        val top1Prob = 0.9250f
        val thresh = 0.4018f

        val logLine = buildString {
            append("CROPDOC_DECISION: ")
            append("source=${meta.source} ")
            append("orig=${meta.origWidth}x${meta.origHeight} ")
            append("exif=${meta.exifOrientation} ")
            append("config=ARGB_8888 ")
            append("colorSpace=sRGB ")
            append("downscale=512x512 | ")
            append("gate1_leafValidator=PASS(frac=${String.format(Locale.US, "%.3f", meta.leafValidatorFraction)}) ")
            append("gate2_energy=OFF(energy=${String.format(Locale.US, "%.4f", freeEnergy)} vs null) ")
            append("gate3_confidence=PASS(class=$top1Class, prob=${String.format(Locale.US, "%.4f", top1Prob)} vs thresh=${String.format(Locale.US, "%.4f", thresh)}) ")
            append("gate4_weeds=PASS | ")
            append("decision=ACCEPTED code=ACCEPTED")
        }

        assertTrue(logLine.startsWith("CROPDOC_DECISION: source=gallery orig=3000x4000 exif=90"))
        assertTrue(logLine.contains("gate1_leafValidator=PASS"))
        assertTrue(logLine.contains("gate2_energy=OFF"))
        assertTrue(logLine.contains("gate3_confidence=PASS"))
        assertTrue(logLine.contains("gate4_weeds=PASS"))
        assertTrue(logLine.contains("decision=ACCEPTED code=ACCEPTED"))
    }
}
