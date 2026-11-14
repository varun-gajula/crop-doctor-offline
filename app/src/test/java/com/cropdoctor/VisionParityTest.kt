package com.cropdoctor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.vision.VisionMath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.abs

@Serializable
data class ParitySample(
    @SerialName("sample_id") val sampleId: String,
    val description: String = "",
    @SerialName("crop_hint") val cropHint: String = "auto",
    val logits: List<Float>,
    val temperature: Float = 1.15f,
    @SerialName("expected_top1_label") val expectedTop1Label: String,
    @SerialName("expected_top1_prob") val expectedTop1Prob: Float
)

class VisionParityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
        "cotton_leaf_curl_virus" to 0.40f,
        "cotton_healthy" to 0.50f,
        "chilli_bacterial_spot" to 0.40f,
        "chilli_healthy" to 0.50f,
        "tomato_early_blight" to 0.40f,
        "tomato_healthy" to 0.50f,
        "maize_common_rust" to 0.40f,
        "maize_healthy" to 0.50f
    )

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
                File("app/src/test/resources/parity_samples.json")
            )
            val found = candidates.firstOrNull { it.exists() }
            assertNotNull("parity_samples.json must be present in ml/ or test resources", found)
            found!!.readText()
        }

        val samples: List<ParitySample> = json.decodeFromString(text)
        assertTrue("Must have at least 5 parity samples", samples.size >= 5)

        for (sample in samples) {
            val sampleId = sample.sampleId
            val cropHint = sample.cropHint
            val temp = sample.temperature
            val expectedLabel = sample.expectedTop1Label
            val expectedProb = sample.expectedTop1Prob
            val logits = sample.logits.toFloatArray()

            // 1. Temperature scaling
            val scaledLogits = VisionMath.applyTemperature(logits, temp)

            // 2. Crop chip masking & re-normalisation
            val probs = VisionMath.maskAndRenormalize(labels, scaledLogits, cropHint)

            // 3. Top-k extraction
            val topK = VisionMath.extractTopK(labels, probs, 3)
            assertEquals("Top-3 must return 3 candidates for sample $sampleId", 3, topK.size)

            val top1 = topK.first()
            assertEquals("Top-1 label mismatch for $sampleId", expectedLabel, top1.classId)
            assertTrue(
                "Probability mismatch for $sampleId: expected $expectedProb, got ${top1.probability}",
                abs(top1.probability - expectedProb) <= 0.02f
            )
        }
    }

    @Test
    fun testCropChipMaskingReNormalization() {
        val logits = FloatArray(25) { 1.0f }
        val scaled = VisionMath.applyTemperature(logits, 1.15f)

        // 1. When Rice is selected, only the 5 rice classes get probabilities and sum to 1.0
        val riceProbs = VisionMath.maskAndRenormalize(labels, scaled, "rice")
        var riceSum = 0.0f
        var nonRiceSum = 0.0f
        for (i in labels.indices) {
            if (labels[i].startsWith("rice_")) {
                riceSum += riceProbs[i]
                assertEquals(0.20f, riceProbs[i], 0.001f)
            } else {
                nonRiceSum += riceProbs[i]
            }
        }
        assertEquals("Rice classes must sum to 1.0", 1.0f, riceSum, 0.001f)
        assertEquals("Non-rice classes must be 0.0", 0.0f, nonRiceSum, 0.0001f)

        // 2. When Cotton is selected, only the 5 cotton classes get probabilities and sum to 1.0
        val cottonProbs = VisionMath.maskAndRenormalize(labels, scaled, "cotton")
        var cottonSum = 0.0f
        var nonCottonSum = 0.0f
        for (i in labels.indices) {
            if (labels[i].startsWith("cotton_")) {
                cottonSum += cottonProbs[i]
                assertEquals(0.20f, cottonProbs[i], 0.001f)
            } else {
                nonCottonSum += cottonProbs[i]
            }
        }
        assertEquals("Cotton classes must sum to 1.0", 1.0f, cottonSum, 0.001f)
        assertEquals("Non-cotton classes must be 0.0", 0.0f, nonCottonSum, 0.0001f)

        // 3. When Auto / All is selected, all 25 classes get probabilities and sum to 1.0
        val autoProbs = VisionMath.maskAndRenormalize(labels, scaled, "auto")
        val autoSum = autoProbs.sum()
        assertEquals("Auto classes must sum to 1.0", 1.0f, autoSum, 0.001f)
        for (p in autoProbs) {
            assertEquals(1.0f / 25.0f, p, 0.001f)
        }
    }

    @Test
    fun testTop3OutputOrdering() {
        val probs = FloatArray(25) { 0.01f }
        probs[2] = 0.65f  // rice_leaf_blast
        probs[1] = 0.20f  // rice_brown_spot
        probs[0] = 0.10f  // rice_bacterial_leaf_blight

        val top3 = VisionMath.extractTopK(labels, probs, 3)
        assertEquals(3, top3.size)
        assertEquals("rice_leaf_blast", top3[0].classId)
        assertEquals(0.65f, top3[0].probability, 0.001f)

        assertEquals("rice_brown_spot", top3[1].classId)
        assertEquals(0.20f, top3[1].probability, 0.001f)

        assertEquals("rice_bacterial_leaf_blight", top3[2].classId)
        assertEquals(0.10f, top3[2].probability, 0.001f)
    }

    @Test
    fun testLowConfidenceRoutesToNotSure() {
        // Logits resulting in low probability below threshold (e.g. 0.25 < 0.40)
        val probs = FloatArray(25) { 0.04f }

        val eval = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probs,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = -5.0, // Normal in-distribution energy
            energyThreshold = -1.5
        )

        assertTrue("Must be marked below threshold", eval.isBelowThreshold)
        assertEquals("unknown", eval.classId)
        assertEquals("Not sure - ask an extension officer", eval.displayName)
        assertEquals(CertaintyBand.NOT_SURE, eval.certaintyBand)
    }

    @Test
    fun testHighEnergyOODRoutesToNotSure() {
        // High confidence class but high free energy (OOD / random noise)
        val probs = FloatArray(25) { 0.0f }
        probs[0] = 0.90f
        probs[1] = 0.10f

        val eval = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = probs,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = 0.5, // Free energy > -1.5 -> OOD!
            energyThreshold = -1.5
        )

        assertTrue("Must be marked out of distribution", eval.isOutOfDistribution)
        assertEquals("unknown", eval.classId)
        assertEquals("Not sure - ask an extension officer", eval.displayName)
        assertEquals(CertaintyBand.NOT_SURE, eval.certaintyBand)
    }
}
