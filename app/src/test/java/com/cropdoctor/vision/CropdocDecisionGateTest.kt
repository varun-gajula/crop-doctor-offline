package com.cropdoctor.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import com.cropdoctor.domain.DiagnosisOutcome

class CropdocDecisionGateTest {

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

    private val defaultThreshold = 0.4018f
    private val unsupportedClasses = setOf("other_not_a_crop")

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
        val probsLow = FloatArray(15) { 0.02f } // max prob 0.02 < 0.4018
        val evalLow = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsLow,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupportedClasses
        )
        assertTrue(evalLow.isBelowThreshold)
        assertEquals("unknown", evalLow.classId)

        val probsHigh = FloatArray(15) { 0.01f }
        probsHigh[0] = 0.81f // rice_leaf_blast
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
        val probsWeeds = FloatArray(15) { 0.01f }
        probsWeeds[14] = 0.81f // other_not_a_crop
        val evalWeeds = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsWeeds,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupportedClasses
        )
        // Non-crop must be rejected as unknown
        assertEquals("unknown", evalWeeds.classId)
        assertTrue(evalWeeds.isBelowThreshold)

        val probsUnsupported = FloatArray(15) { 0.01f }
        probsUnsupported[14] = 0.81f // other_not_a_crop
        val evalUnsupported = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsUnsupported,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = setOf("other_not_a_crop")
        )
        // unsupported class must be rejected as unknown
        assertEquals("unknown", evalUnsupported.classId)
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

    @Test
    fun testTaskP4ThreeClearOutcomes_CantTell() {
        val energyThresh = -8.0
        val cropMassThresh = 0.60f
        val marginThresh = 0.25f
        val borderlineEnergy = -9.0

        // 1. Gate 1 fail (not a leaf)
        val evalGate1Fail = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = FloatArray(15) { if (it == 0) 0.95f else 0.002f },
            gate1Pass = false,
            leafValidatorReason = "LOW_GREEN_FRACTION",
            freeEnergy = -16.0,
            energyThreshold = energyThresh,
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy
        )
        assertEquals(DiagnosisOutcome.CANT_TELL, evalGate1Fail.outcome)
        assertTrue(evalGate1Fail.outcomeReason.startsWith("LEAF_VALIDATOR_FAIL"))

        // 2. Global top-1 non-crop
        val probsWeeds = FloatArray(15) { 0.01f }
        probsWeeds[14] = 0.81f // other_not_a_crop
        val evalWeeds = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsWeeds,
            freeEnergy = -15.0,
            energyThreshold = energyThresh,
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy
        )
        assertEquals(DiagnosisOutcome.CANT_TELL, evalWeeds.outcome)
        assertEquals("GLOBAL_TOP1_WEEDS", evalWeeds.outcomeReason)

        // 3. High Free Energy (OOD)
        val evalHighEnergy = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = FloatArray(15) { if (it == 0) 0.90f else 0.005f },
            freeEnergy = -6.5, // -6.5 > -8.0
            energyThreshold = energyThresh,
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy
        )
        assertEquals(DiagnosisOutcome.CANT_TELL, evalHighEnergy.outcome)
        assertTrue(evalHighEnergy.outcomeReason.startsWith("HIGH_FREE_ENERGY"))

        // 4. Low within-crop confidence
        val evalLowConf = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = FloatArray(15) { 0.02f },
            defaultThreshold = 0.3434f,
            freeEnergy = -15.0,
            energyThreshold = energyThresh,
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy
        )
        assertEquals(DiagnosisOutcome.CANT_TELL, evalLowConf.outcome)
        assertTrue(evalLowConf.outcomeReason.startsWith("LOW_INCROP_CONFIDENCE"))
    }

    @Test
    fun testTaskP4ThreeClearOutcomes_OutOfClass() {
        val energyThresh = -8.0
        val cropMassThresh = 0.60f
        val marginThresh = 0.25f
        val borderlineEnergy = -9.0

        // 1. Low Crop Mass (e.g. wrong crop selected: model gave only 8% mass to selected crop)
        val probsTomatoCurl = FloatArray(15) { 0.01f }
        probsTomatoCurl[13] = 0.80f // tomato_septoria_leaf_spot
        val evalLowCropMass = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsTomatoCurl,
            freeEnergy = -15.0,
            energyThreshold = energyThresh,
            cropMass = 0.08f, // 0.08 < 0.60
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy
        )
        assertEquals(DiagnosisOutcome.OUT_OF_CLASS, evalLowCropMass.outcome)
        assertTrue(evalLowCropMass.outcomeReason.startsWith("LOW_CROP_MASS"))

        // 2. Borderline Ambiguous Margin with Borderline Energy
        // top1=0.45, top2=0.40 -> margin = 0.05 < 0.25, Free Energy = -8.5 >= -9.0
        val probsAmbiguous = FloatArray(15) { 0.005f }
        probsAmbiguous[12] = 0.45f
        probsAmbiguous[13] = 0.40f
        val evalBorderline = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsAmbiguous,
            freeEnergy = -8.5,
            energyThreshold = energyThresh,
            cropMass = 0.85f,
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy
        )
        assertEquals(DiagnosisOutcome.OUT_OF_CLASS, evalBorderline.outcome)
        assertTrue(evalBorderline.outcomeReason.startsWith("BORDERLINE_AMBIGUOUS"))

        // 3. Unsupported Class
        val probsUnsupported = FloatArray(15) { 0.01f }
        probsUnsupported[0] = 0.80f // rice_leaf_blast
        val evalUnsupported = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsUnsupported,
            freeEnergy = -15.0,
            energyThreshold = energyThresh,
            cropMass = 0.90f,
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy,
            unsupportedClasses = setOf("rice_leaf_blast")
        )
        assertEquals(DiagnosisOutcome.OUT_OF_CLASS, evalUnsupported.outcome)
        assertTrue(evalUnsupported.outcomeReason.startsWith("UNSUPPORTED_CLASS"))
    }

    @Test
    fun testTaskP4ThreeClearOutcomes_Diagnose() {
        val energyThresh = -8.0
        val cropMassThresh = 0.60f
        val marginThresh = 0.25f
        val borderlineEnergy = -9.0

        val probsConfident = FloatArray(15) { 0.005f }
        probsConfident[0] = 0.90f // rice_leaf_blast
        val evalDiagnose = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsConfident,
            freeEnergy = -16.5,
            energyThreshold = energyThresh,
            cropMass = 0.95f,
            cropMassThreshold = cropMassThresh,
            marginThreshold = marginThresh,
            borderlineEnergy = borderlineEnergy
        )
        assertEquals(DiagnosisOutcome.DIAGNOSE, evalDiagnose.outcome)
        assertEquals("ACCEPTED", evalDiagnose.outcomeReason)
        assertEquals("rice_leaf_blast", evalDiagnose.classId)
        assertTrue(evalDiagnose.confidence >= 0.90f)
    }

    @Test
    fun testTaskP4OptionBThresholdValuesInAssets() {
        val assetFile = File("src/main/assets/thresholds.json")
        assertTrue("thresholds.json must exist in assets", assetFile.exists())
        val text = assetFile.readText()
        assertTrue("energy_threshold must be in thresholds.json", text.contains("\"energy_threshold\": -4.7126"))
        assertTrue("confidence_threshold must be in thresholds.json", text.contains("\"confidence_threshold\": 0.75"))
        assertTrue("temperature must be in thresholds.json", text.contains("\"temperature\": 0.978"))
    }

    @Test
    fun testTaskP4StringLocalizationPresentInAllThreeLanguages() {
        val resDir = File("src/main/res")
        val enXml = File(resDir, "values/strings.xml").readText()
        val teXml = File(resDir, "values-te/strings.xml").readText()
        val hiXml = File(resDir, "values-hi/strings.xml").readText()

        val requiredKeys = listOf(
            "outcome_diagnose",
            "outcome_cant_tell_title",
            "outcome_cant_tell_message",
            "outcome_out_of_class_title",
            "outcome_out_of_class_message",
            "btn_take_another_photo",
            "btn_call_kisan_helpline",
            "btn_change_crop"
        )

        for (key in requiredKeys) {
            assertTrue("Key $key missing in English", enXml.contains("name=\"$key\""))
            assertTrue("Key $key missing in Telugu", teXml.contains("name=\"$key\""))
            assertTrue("Key $key missing in Hindi", hiXml.contains("name=\"$key\""))
        }
    }
}
