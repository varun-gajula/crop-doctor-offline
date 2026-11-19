package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Task V7 Unit Tests: 20-Class Model Direct Output Mapping & Unsupported Class Handling
 * (a) Model output size equals number of labels in labels.json (20).
 * (b) Each of the 20 labels corresponds directly 1-to-1 to the model output index.
 * (c) Top-1 in an unsupported class (weeds_not_a_crop, tomato_leaf_blight) gives "Can't tell" (unknown / NOT_SURE).
 * (d) Known cases: index 16 (maize_common_rust) maps to maize_common_rust; index 0 maps to rice_bacterial_leaf_blight.
 */
class ModelClassMapTest {

    private fun findAsset(name: String): File {
        val candidates = listOf(
            File("app/src/main/assets/$name"),
            File("src/main/assets/$name"),
            File("../app/src/main/assets/$name"),
            File("app/src/test/resources/$name"),
            File("src/test/resources/$name")
        )
        val f = candidates.firstOrNull { it.exists() }
        assertNotNull("Could not find asset $name in any standard path", f)
        return f!!
    }

    private fun loadLabels(): List<String> {
        val file = findAsset("labels.json")
        val jsonArray = Json.parseToJsonElement(file.readText()).jsonArray
        return jsonArray.map { it.jsonPrimitive.content.trim() }
    }

    @Test
    fun testModelOutputSizeEqualsEntriesInLabelsJson() {
        val labels = loadLabels()
        assertEquals("labels.json must have exactly 20 classes", 20, labels.size)
        assertEquals(
            "Model output size constant must equal 20",
            20,
            ExecuTorchClassifier.MODEL_OUTPUT_SIZE
        )
        assertEquals(
            "VisionMath MODEL_OUTPUT_SIZE must equal 20",
            20,
            VisionMath.MODEL_OUTPUT_SIZE
        )
    }

    @Test
    fun testEachOf20LabelsDirectlyMapped() {
        val labels = loadLabels()
        assertEquals(20, labels.size)

        val expectedLabels = listOf(
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

        for (i in expectedLabels.indices) {
            assertEquals("Index $i must match expected label", expectedLabels[i], labels[i])
        }
    }

    @Test
    fun testUnsupportedClassesGiveCantTell() {
        val labels = loadLabels()
        val unsupported = setOf("weeds_not_a_crop", "tomato_leaf_blight")

        for (unsupportedClass in unsupported) {
            val idx = labels.indexOf(unsupportedClass)
            assertTrue("Class $unsupportedClass must be present in labels.json", idx >= 0)

            // Construct 20 logits where unsupportedClass is dominant (top-1)
            val logits = FloatArray(20) { -5.0f }
            logits[idx] = 10.0f

            val scaledLogits = VisionMath.applyTemperature(logits, 1.3302f)
            val probs = VisionMath.softmax(scaledLogits)

            val eval = VisionMath.evaluateDiagnosis(
                labels = labels,
                probabilities = probs,
                thresholds = emptyMap(),
                defaultThreshold = 0.4018f,
                freeEnergy = -5.0,
                energyThreshold = null,
                unsupportedClasses = unsupported
            )

            assertEquals("Unsupported class must yield classId 'unknown'", "unknown", eval.classId)
            assertEquals("Unsupported class must show referral text", "Not sure - ask an extension officer", eval.displayName)
            assertEquals("Unsupported class must have certainty NOT_SURE", CertaintyBand.NOT_SURE, eval.certaintyBand)
            assertTrue("isBelowThreshold must be true for unsupported class", eval.isBelowThreshold)
        }
    }

    @Test
    fun testKnownCasesDirectMapping() {
        val labels = loadLabels()

        // 1. Maize common rust (index 16)
        val rustIdx = labels.indexOf("maize_common_rust")
        assertEquals(16, rustIdx)

        val rustLogits = FloatArray(20) { -5.0f }
        rustLogits[rustIdx] = 8.0f

        val rustProbs = VisionMath.softmax(VisionMath.applyTemperature(rustLogits, 1.3302f))
        val evalRust = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = rustProbs,
            thresholds = emptyMap(),
            defaultThreshold = 0.4018f,
            freeEnergy = -5.0,
            energyThreshold = null,
            unsupportedClasses = setOf("weeds_not_a_crop", "tomato_leaf_blight")
        )

        assertEquals("maize_common_rust", evalRust.classId)
        assertEquals("Maize Common Rust", evalRust.displayName)
        assertEquals(CertaintyBand.SURE, evalRust.certaintyBand)

        // 2. Rice bacterial leaf blight (index 0)
        val blbIdx = labels.indexOf("rice_bacterial_leaf_blight")
        assertEquals(0, blbIdx)

        val blbLogits = FloatArray(20) { -5.0f }
        blbLogits[blbIdx] = 8.0f

        val blbProbs = VisionMath.softmax(VisionMath.applyTemperature(blbLogits, 1.3302f))
        val evalBlb = VisionMath.evaluateDiagnosis(
            labels = labels,
            probabilities = blbProbs,
            thresholds = emptyMap(),
            defaultThreshold = 0.4018f,
            freeEnergy = -5.0,
            energyThreshold = null,
            unsupportedClasses = setOf("weeds_not_a_crop", "tomato_leaf_blight")
        )

        assertEquals("rice_bacterial_leaf_blight", evalBlb.classId)
        assertEquals("Rice Bacterial Leaf Blight", evalBlb.displayName)
        assertEquals(CertaintyBand.SURE, evalBlb.certaintyBand)
    }
}
