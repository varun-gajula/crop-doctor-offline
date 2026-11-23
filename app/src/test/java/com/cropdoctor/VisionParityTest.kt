package com.cropdoctor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.vision.VisionMath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VisionParityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
    private val temperature = 1.3302f

    @Test
    fun testParitySamplesFromColab() {
        val stream = javaClass.classLoader?.getResourceAsStream("parity_samples.json")
        val text = if (stream != null) {
            stream.bufferedReader().use { it.readText() }
        } else {
            val candidates = listOf(
                File("ml/parity_samples.json"),
                File("../ml/parity_samples.json"),
                File("src/test/resources/parity_samples.json"),
                File("app/src/test/resources/parity_samples.json"),
                File("benchmarks/final/parity_samples.json")
            )
            val found = candidates.firstOrNull { it.exists() }
            assertNotNull("parity_samples.json must be present in ml/ or test resources", found)
            found!!.readText()
        }

        val jsonElement = Json.parseToJsonElement(text).jsonObject
        assertTrue("parity_samples must have all_pass true", jsonElement["all_pass"]!!.jsonPrimitive.boolean)

        val tol = (jsonElement["tolerance"] ?: jsonElement["gate_threshold"])!!.jsonPrimitive.double
        assertTrue("Tolerance must be 1e-3 or stricter", tol <= 0.001)

        val inShape = jsonElement["input_shape"]?.jsonArray?.map { it.jsonPrimitive.int }
        if (inShape != null) {
            assertEquals(listOf(1, 3, 512, 512), inShape)
        }

        val outShape = jsonElement["output_shape"]?.jsonArray?.map { it.jsonPrimitive.int }
        if (outShape != null) {
            assertEquals(listOf(1, 15), outShape)
        }

        val results = (jsonElement["records"] ?: jsonElement["results"])!!.jsonArray
        assertTrue("Must test at least 5 parity samples", results.size >= 5)

        for (sample in results) {
            val sampleObj = sample.jsonObject
            val maxErr = (sampleObj["max_abs_error"] ?: sampleObj["max_absolute_error"])!!.jsonPrimitive.double
            val passed = sampleObj["pass"]!!.jsonPrimitive.boolean
            val sampleId = sampleObj["sample_id"] ?: sampleObj["image"]
            assertTrue("Sample $sampleId must pass parity check", passed)
            assertTrue("Max absolute error must be <= 1e-3: got $maxErr", maxErr <= 0.001)
        }
    }

    @Test
    fun testCropChipMaskingReNormalization() {
        val logits = FloatArray(15) { 1.0f }
        val scaled = VisionMath.applyTemperature(logits, temperature)

        // 1. When Rice is selected, only the 4 rice classes get probabilities and sum to 1.0
        val riceProbs = VisionMath.maskAndRenormalize(labels, scaled, "rice")
        var riceSum = 0.0f
        var nonRiceSum = 0.0f
        for (i in labels.indices) {
            if (labels[i].startsWith("rice_")) {
                riceSum += riceProbs[i]
                assertEquals(0.25f, riceProbs[i], 0.001f)
            } else {
                nonRiceSum += riceProbs[i]
            }
        }
        assertEquals("Rice classes must sum to 1.0", 1.0f, riceSum, 0.001f)
        assertEquals("Non-rice classes must be 0.0", 0.0f, nonRiceSum, 0.0001f)

        // 2. When Cotton is selected, only the 2 cotton classes get probabilities and sum to 1.0
        val cottonProbs = VisionMath.maskAndRenormalize(labels, scaled, "cotton")
        var cottonSum = 0.0f
        var nonCottonSum = 0.0f
        for (i in labels.indices) {
            if (labels[i].startsWith("cotton_")) {
                cottonSum += cottonProbs[i]
                assertEquals(0.50f, cottonProbs[i], 0.001f)
            } else {
                nonCottonSum += cottonProbs[i]
            }
        }
        assertEquals("Cotton classes must sum to 1.0", 1.0f, cottonSum, 0.001f)
        assertEquals("Non-cotton classes must be 0.0", 0.0f, nonCottonSum, 0.0001f)

        // 3. When Auto / All is selected, all 15 classes get probabilities and sum to 1.0
        val autoProbs = VisionMath.maskAndRenormalize(labels, scaled, "auto")
        val autoSum = autoProbs.sum()
        assertEquals("Auto classes must sum to 1.0", 1.0f, autoSum, 0.001f)
        for (p in autoProbs) {
            assertEquals(1.0f / 15.0f, p, 0.001f)
        }
    }

    @Test
    fun testTop3OutputOrdering() {
        val probs = FloatArray(15) { 0.01f }
        probs[0] = 0.65f  // rice_leaf_blast
        probs[1] = 0.20f  // rice_healthy
        probs[2] = 0.10f  // cotton_bacterial_blight

        val top3 = VisionMath.extractTopK(labels, probs, 3)
        assertEquals(3, top3.size)
        assertEquals("rice_leaf_blast", top3[0].classId)
        assertEquals(0.65f, top3[0].probability, 0.001f)

        assertEquals("rice_healthy", top3[1].classId)
        assertEquals(0.20f, top3[1].probability, 0.001f)

        assertEquals("cotton_bacterial_blight", top3[2].classId)
        assertEquals(0.10f, top3[2].probability, 0.001f)
    }

    @Test
    fun testLowConfidenceRoutesToCantTell() {
        // Uniform distribution: 1/15 = 0.0667 < 0.4018
        val probs = FloatArray(15) { 1.0f / 15.0f }

        val eval = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probs,
            thresholds = emptyMap(),
            defaultThreshold = defaultThreshold,
            freeEnergy = -5.0,
            energyThreshold = null
        )

        assertTrue("Must be marked below threshold", eval.isBelowThreshold)
        assertEquals("unknown", eval.classId)
        assertEquals("Not sure - ask an extension officer", eval.displayName)
        assertEquals(CertaintyBand.NOT_SURE, eval.certaintyBand)
    }

    @Test
    fun testCertaintyBandsThresholds() {
        val unsupported = setOf("other_not_a_crop")

        // 1. High confidence >= 0.80 -> SURE
        val probsSure = FloatArray(15) { 0.005f }
        probsSure[0] = 0.85f
        val evalSure = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsSure,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupported
        )
        assertEquals(CertaintyBand.SURE, evalSure.certaintyBand)
        assertEquals("rice_leaf_blast", evalSure.classId)

        // 2. Medium confidence 0.60..0.80 -> PROBABLY
        val probsProb = FloatArray(15) { 0.015f }
        probsProb[0] = 0.70f
        val evalProb = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsProb,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupported
        )
        assertEquals(CertaintyBand.PROBABLY, evalProb.certaintyBand)
        assertEquals("rice_leaf_blast", evalProb.classId)

        // 3. Low confidence between threshold and 0.60 (0.45 >= 0.4018) -> NOT_SURE
        val probsLow = FloatArray(15) { 0.025f }
        probsLow[0] = 0.45f
        val evalLow = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsLow,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupported
        )
        assertEquals(CertaintyBand.NOT_SURE, evalLow.certaintyBand)
        assertEquals("rice_leaf_blast", evalLow.classId)

        // 4. Below confidence threshold (< 0.4018) -> "Can't tell" (unknown, NOT_SURE)
        val probsBelow = FloatArray(15) { 0.035f }
        probsBelow[0] = 0.35f
        val evalBelow = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probsBelow,
            defaultThreshold = defaultThreshold,
            unsupportedClasses = unsupported
        )
        assertEquals("unknown", evalBelow.classId)
        assertEquals(CertaintyBand.NOT_SURE, evalBelow.certaintyBand)
    }
}
