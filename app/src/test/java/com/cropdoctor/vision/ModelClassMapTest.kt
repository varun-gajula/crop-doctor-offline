package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Task V5b Unit Tests: Model-to-Label Mapping (25 Model Outputs to 21 App Labels)
 * (a) Model output size equals number of entries in model_class_map.json (25).
 * (b) Each of the 21 labels is reachable exactly once.
 * (c) Top-1 in a dropped class (original indices 7, 8, 12, 13) gives "Can't tell" (unknown / NOT_SURE).
 * (d) Known case: original index 21 (maize_common_rust) maps to the label named maize_common_rust.
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

    private fun loadClassMap(): List<Int?> {
        val file = findAsset("model_class_map.json")
        val jsonArray = Json.parseToJsonElement(file.readText()).jsonArray
        return jsonArray.map { elem ->
            val prim = elem.jsonPrimitive
            if (prim.isString) {
                null
            } else {
                try {
                    prim.content.toInt()
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private fun loadLabels(): List<String> {
        val file = findAsset("labels.json")
        val jsonArray = Json.parseToJsonElement(file.readText()).jsonArray
        return jsonArray.map { it.jsonPrimitive.content.trim() }
    }

    @Test
    fun testModelOutputSizeEqualsEntriesInModelClassMap() {
        val classMap = loadClassMap()
        // (a) the model output size read from the model or from a constant equals the number of entries in model_class_map.json
        assertEquals(
            "Model output size constant must equal number of entries in model_class_map.json",
            ExecuTorchClassifier.MODEL_OUTPUT_SIZE,
            classMap.size
        )
        assertEquals("model_class_map.json must have exactly 25 entries", 25, classMap.size)
    }

    @Test
    fun testEachOf21LabelsReachableExactlyOnce() {
        val classMap = loadClassMap()
        val labels = loadLabels()
        assertEquals("labels.json must have 21 classes", 21, labels.size)

        // (b) each of the 21 labels is reachable exactly once
        val nonNullIndices = classMap.filterNotNull()
        assertEquals("Exactly 21 model outputs must map to valid classes", 21, nonNullIndices.size)

        val uniqueIndices = nonNullIndices.toSet()
        assertEquals("Each index must be mapped uniquely", 21, uniqueIndices.size)
        assertEquals("Mapped indices must be 0..20", (0..20).toSet(), uniqueIndices)

        val reachedLabels = nonNullIndices.map { labels[it] }
        assertEquals("All 21 labels must be reached exactly once", labels.toSet(), reachedLabels.toSet())
    }

    @Test
    fun testTop1InDroppedClassGivesCantTell() {
        val classMap = loadClassMap()
        val labels = loadLabels()

        val droppedIndices = listOf(7, 8, 12, 13)
        for (droppedIdx in droppedIndices) {
            assertNull("Index $droppedIdx must be null in class map", classMap[droppedIdx])

            // Construct 25 logits where droppedIdx is dominant (top-1)
            val logits = FloatArray(25) { -5.0f }
            logits[droppedIdx] = 10.0f // Very high confidence for dropped class

            val eval = VisionMath.evaluateModel25Output(
                rawLogits = logits,
                modelClassMap = classMap,
                labels21 = labels,
                temperature = 1.15f,
                thresholds = emptyMap(),
                defaultThreshold = 0.20f,
                energyThreshold = 5.0
            )

            // (c) top-1 in a dropped class gives "Can't tell"
            assertEquals("Dropped class must yield classId 'unknown'", "unknown", eval.classId)
            assertEquals("Dropped class must show 'Not sure - ask an extension officer'", "Not sure - ask an extension officer", eval.displayName)
            assertEquals("Dropped class must have certainty NOT_SURE", CertaintyBand.NOT_SURE, eval.certaintyBand)
            assertTrue("isBelowThreshold must be true for dropped class", eval.isBelowThreshold)
        }
    }

    @Test
    fun testKnownCaseMaizeCommonRust() {
        val classMap = loadClassMap()
        val labels = loadLabels()

        // (d) a known case: original index 21 (maize_common_rust) maps to the label named maize_common_rust
        val targetIdx = classMap[21]
        assertNotNull("Original index 21 must not be null", targetIdx)
        assertEquals("Original index 21 must map to maize_common_rust", "maize_common_rust", labels[targetIdx!!])

        // Verify inference simulation with top-1 at index 21
        val logits = FloatArray(25) { -5.0f }
        logits[21] = 8.0f // Maize common rust

        val eval = VisionMath.evaluateModel25Output(
            rawLogits = logits,
            modelClassMap = classMap,
            labels21 = labels,
            temperature = 1.15f,
            thresholds = mapOf("maize_common_rust" to 0.20f),
            defaultThreshold = 0.20f,
            energyThreshold = 5.0
        )

        assertEquals("Diagnosis classId must be maize_common_rust", "maize_common_rust", eval.classId)
        assertEquals("Display name must be Maize Common Rust", "Maize Common Rust", eval.displayName)
        assertNotEquals("Certainty must not be NOT_SURE", CertaintyBand.NOT_SURE, eval.certaintyBand)
        assertFalse("Must not be OOD", eval.isOutOfDistribution)
        assertFalse("Must not be below threshold", eval.isBelowThreshold)
    }
}
