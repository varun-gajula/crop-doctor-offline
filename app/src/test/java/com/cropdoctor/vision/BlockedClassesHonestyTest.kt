package com.cropdoctor.vision

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Task V2 Unit Tests: Blocked & Unsupported Classes
 * 1. data/supported_classes.json marks classes 7, 8, 12, 13 as supported=false with required reasons.
 * 2. Classifier / VisionMath treats a top prediction in an unsupported class as "Can't tell" (unknown / NOT_SURE).
 * 3. Does not retrain or remove them from the 25-way model output.
 */
class BlockedClassesHonestyTest {

    private val all25Labels = listOf(
        "rice_bacterial_leaf_blight",       // 0
        "rice_brown_spot",                  // 1
        "rice_leaf_blast",                  // 2
        "rice_sheath_blight",               // 3
        "rice_healthy",                     // 4
        "cotton_bacterial_blight",          // 5
        "cotton_fusarium_wilt",             // 6
        "cotton_leaf_curl_virus",           // 7  (UNSUPPORTED)
        "cotton_alternaria_leaf_spot",      // 8  (UNSUPPORTED)
        "cotton_healthy",                   // 9
        "chilli_bacterial_spot",            // 10
        "chilli_leaf_curl_virus",           // 11
        "chilli_cercospora_leaf_spot",       // 12 (UNSUPPORTED)
        "chilli_white_spot",                // 13 (UNSUPPORTED)
        "chilli_healthy",                   // 14
        "tomato_early_blight",              // 15
        "tomato_late_blight",               // 16
        "tomato_yellow_leaf_curl_virus",    // 17
        "tomato_spider_mites",              // 18
        "tomato_healthy",                   // 19
        "maize_gray_leaf_spot",             // 20
        "maize_common_rust",                // 21
        "maize_northern_leaf_blight",       // 22
        "maize_fall_armyworm",              // 23
        "maize_healthy"                     // 24
    )

    private val thresholds = mapOf(
        "rice_bacterial_leaf_blight" to 0.40f,
        "cotton_leaf_curl_virus" to 0.40f,
        "cotton_alternaria_leaf_spot" to 0.40f,
        "chilli_cercospora_leaf_spot" to 0.40f,
        "chilli_white_spot" to 0.40f,
        "tomato_early_blight" to 0.40f
    )

    private fun loadUnsupportedClassIds(): Set<String> {
        val candidates = listOf(
            File("src/main/assets/supported_classes.json"),
            File("../data/supported_classes.json"),
            File("data/supported_classes.json")
        )
        val target = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find supported_classes.json in $candidates")

        val json = Json.parseToJsonElement(target.readText()).jsonObject
        val unsupported = mutableSetOf<String>()
        val classesObj = json["classes"]!!.jsonObject
        for ((_, entryEl) in classesObj) {
            val entry = entryEl.jsonObject
            if (entry.containsKey("supported") && !entry["supported"]!!.jsonPrimitive.boolean) {
                unsupported.add(entry["class_id"]!!.jsonPrimitive.content)
            }
        }
        return unsupported
    }

    @Test
    fun testSupportedClassesJsonMarksClasses7_8_12_13AsUnsupported() {
        val candidates = listOf(
            File("src/main/assets/supported_classes.json"),
            File("../data/supported_classes.json"),
            File("data/supported_classes.json")
        )
        val target = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find supported_classes.json in $candidates")

        val json = Json.parseToJsonElement(target.readText()).jsonObject
        val classesObj = json["classes"]!!.jsonObject

        // 1. Verify class 7 (cotton_leaf_curl_virus)
        val c7 = classesObj["7"]!!.jsonObject
        assertEquals("cotton_leaf_curl_virus", c7["class_id"]!!.jsonPrimitive.content)
        assertFalse("Class 7 must have supported=false", c7["supported"]!!.jsonPrimitive.boolean)
        assertTrue(
            "Class 7 reason must state fewer than 300 unique photos",
            c7["reason"]!!.jsonPrimitive.content.contains("fewer than 300 unique photos")
        )

        // 2. Verify class 8 (cotton_alternaria_leaf_spot)
        val c8 = classesObj["8"]!!.jsonObject
        assertEquals("cotton_alternaria_leaf_spot", c8["class_id"]!!.jsonPrimitive.content)
        assertFalse("Class 8 must have supported=false", c8["supported"]!!.jsonPrimitive.boolean)
        assertTrue(
            "Class 8 reason must state fewer than 300 unique photos",
            c8["reason"]!!.jsonPrimitive.content.contains("fewer than 300 unique photos")
        )

        // 3. Verify class 12 (chilli_cercospora_leaf_spot)
        val c12 = classesObj["12"]!!.jsonObject
        assertEquals("chilli_cercospora_leaf_spot", c12["class_id"]!!.jsonPrimitive.content)
        assertFalse("Class 12 must have supported=false", c12["supported"]!!.jsonPrimitive.boolean)
        assertTrue(
            "Class 12 reason must state fewer than 300 unique photos",
            c12["reason"]!!.jsonPrimitive.content.contains("fewer than 300 unique photos")
        )

        // 4. Verify class 13 (chilli_white_spot)
        val c13 = classesObj["13"]!!.jsonObject
        assertEquals("chilli_white_spot", c13["class_id"]!!.jsonPrimitive.content)
        assertFalse("Class 13 must have supported=false", c13["supported"]!!.jsonPrimitive.boolean)
        assertTrue(
            "Class 13 reason must state fewer than 300 unique photos",
            c13["reason"]!!.jsonPrimitive.content.contains("fewer than 300 unique photos")
        )
        assertTrue(
            "Class 13 reason must state cause unverified",
            c13["reason"]!!.jsonPrimitive.content.contains("cause unverified")
        )

        // 5. Verify total 25 classes and all other 21 classes are supported=true
        assertEquals(25, classesObj.size)
        var supportedCount = 0
        var unsupportedCount = 0
        for (i in 0 until 25) {
            val entry = classesObj[i.toString()]!!.jsonObject
            if (entry["supported"]!!.jsonPrimitive.boolean) {
                supportedCount++
            } else {
                unsupportedCount++
            }
        }
        assertEquals("Exactly 4 unsupported classes", 4, unsupportedCount)
        assertEquals("Exactly 21 supported classes", 21, supportedCount)
    }

    @Test
    fun testTopPredictionInUnsupportedClassTreatedAsCantTell() {
        val unsupported = loadUnsupportedClassIds()
        assertTrue("Must include cotton_leaf_curl_virus", unsupported.contains("cotton_leaf_curl_virus"))
        assertTrue("Must include cotton_alternaria_leaf_spot", unsupported.contains("cotton_alternaria_leaf_spot"))
        assertTrue("Must include chilli_cercospora_leaf_spot", unsupported.contains("chilli_cercospora_leaf_spot"))
        assertTrue("Must include chilli_white_spot", unsupported.contains("chilli_white_spot"))

        val blockedIndices = listOf(7, 8, 12, 13)

        for (blockedIdx in blockedIndices) {
            val blockedClassId = all25Labels[blockedIdx]

            // Simulate confident top prediction (95% confidence) for the blocked class
            val probs = FloatArray(25) { 0.002f }
            probs[blockedIdx] = 0.95f

            val eval = VisionMath.evaluateDiagnosis(
                labels = all25Labels,
                probabilities = probs,
                thresholds = thresholds,
                defaultThreshold = 0.40f,
                freeEnergy = -5.0, // Normal in-distribution energy
                energyThreshold = -1.5,
                unsupportedClasses = unsupported
            )

            // Must strictly return "Can't tell" (unknown / NOT_SURE)
            assertEquals("Top prediction in blocked class $blockedClassId must return 'unknown'", "unknown", eval.classId)
            assertEquals("Top prediction in blocked class $blockedClassId must return NOT_SURE certainty", CertaintyBand.NOT_SURE, eval.certaintyBand)
            assertEquals("Top prediction in blocked class $blockedClassId must return referral displayName", "Not sure - ask an extension officer", eval.displayName)

            // UI rules: when certainty is NOT_SURE, severity should never be shown
            assertFalse("Severity must not be shown for unsupported class", RulesEngine.shouldShowSeverity(eval.certaintyBand))
        }
    }

    @Test
    fun testSupportedClassesAreNotBlocked() {
        val unsupported = loadUnsupportedClassIds()

        // Test Rice Bacterial Leaf Blight (index 0) with 92% confidence
        val probsRice = FloatArray(25) { 0.003f }
        probsRice[0] = 0.92f

        val evalRice = VisionMath.evaluateDiagnosis(
            labels = all25Labels,
            probabilities = probsRice,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = -5.0,
            energyThreshold = -1.5,
            unsupportedClasses = unsupported
        )

        assertEquals("Supported rice class must be diagnosed", "rice_bacterial_leaf_blight", evalRice.classId)
        assertEquals("Supported rice class certainty must be SURE", CertaintyBand.SURE, evalRice.certaintyBand)

        // Test Tomato Early Blight (index 15) with 88% confidence
        val probsTomato = FloatArray(25) { 0.005f }
        probsTomato[15] = 0.88f

        val evalTomato = VisionMath.evaluateDiagnosis(
            labels = all25Labels,
            probabilities = probsTomato,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = -5.0,
            energyThreshold = -1.5,
            unsupportedClasses = unsupported
        )

        assertEquals("Supported tomato class must be diagnosed", "tomato_early_blight", evalTomato.classId)
        assertEquals("Supported tomato class certainty must be SURE", CertaintyBand.SURE, evalTomato.certaintyBand)
    }
}
