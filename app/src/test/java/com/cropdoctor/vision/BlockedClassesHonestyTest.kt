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
 * Task V7 Unit Tests: Verification of 20-Class Architecture & Unsupported Classes Honesty
 * 1. Confirms supported_classes.json has exactly 20 classes (indices 0..19).
 * 2. Confirms weeds_not_a_crop and tomato_leaf_blight are marked supported=false.
 * 3. Confirms the remaining 18 classes are supported=true.
 * 4. Confirms VisionMath safety: if any class is marked unsupported, it returns "Can't tell" (unknown / NOT_SURE).
 */
class BlockedClassesHonestyTest {

    private val all20Labels = listOf(
        "rice_bacterial_leaf_blight",       // 0
        "rice_leaf_blast",                  // 1
        "rice_sheath_blight",               // 2
        "rice_healthy",                     // 3
        "cotton_bacterial_blight",          // 4
        "cotton_fusarium_wilt",             // 5
        "cotton_leaf_curl_virus",           // 6
        "cotton_healthy",                   // 7
        "chilli_leaf_curl_virus",           // 8
        "chilli_nutrition_deficiency",      // 9
        "chilli_healthy",                   // 10
        "tomato_leaf_blight",               // 11 (unsupported)
        "tomato_yellow_leaf_curl_virus",    // 12
        "tomato_septoria_leaf_spot",        // 13
        "tomato_healthy",                   // 14
        "maize_gray_leaf_spot",             // 15
        "maize_common_rust",                // 16
        "maize_northern_leaf_blight",       // 17
        "maize_healthy",                    // 18
        "weeds_not_a_crop"                  // 19 (unsupported)
    )

    private val unsupportedClassIds = setOf(
        "tomato_leaf_blight",
        "weeds_not_a_crop"
    )

    private fun findFile(relativePaths: List<String>): File {
        for (rel in relativePaths) {
            val f = File(rel)
            if (f.exists()) return f
            val f2 = File("../$rel")
            if (f2.exists()) return f2
        }
        throw IllegalStateException("Could not find any of $relativePaths")
    }

    @Test
    fun testSupportedClassesJsonHasExactly20Classes() {
        val target = findFile(listOf(
            "app/src/main/assets/supported_classes.json",
            "src/main/assets/supported_classes.json",
            "data/supported_classes.json"
        ))

        val json = Json.parseToJsonElement(target.readText()).jsonObject
        val classesObj = json["classes"]!!.jsonObject

        assertEquals("Must have exactly 20 classes", 20, classesObj.size)

        for (i in 0 until 20) {
            val entry = classesObj[i.toString()]!!.jsonObject
            val classId = entry["class_id"]!!.jsonPrimitive.content
            assertEquals("Index $i must match all20Labels[$i]", all20Labels[i], classId)

            val isSupported = entry["supported"]!!.jsonPrimitive.boolean
            if (classId in unsupportedClassIds) {
                assertFalse("Class $classId must have supported=false", isSupported)
            } else {
                assertTrue("Class $classId must have supported=true", isSupported)
            }
        }
    }

    @Test
    fun testSimulatedUnsupportedClassIsTreatedAsCantTell() {
        for (unsupportedClass in unsupportedClassIds) {
            val idx = all20Labels.indexOf(unsupportedClass)
            assertTrue("Class $unsupportedClass must be present in labels", idx >= 0)

            val probs = FloatArray(20) { 0.002f }
            probs[idx] = 0.95f

            val eval = VisionMath.evaluateDiagnosis(
                labels = all20Labels,
                probabilities = probs,
                thresholds = emptyMap(),
                defaultThreshold = 0.4018f,
                freeEnergy = -5.0,
                energyThreshold = null,
                unsupportedClasses = unsupportedClassIds
            )

            assertEquals("Unsupported class $unsupportedClass must return 'unknown'", "unknown", eval.classId)
            assertEquals("Unsupported class must return NOT_SURE certainty", CertaintyBand.NOT_SURE, eval.certaintyBand)
            assertEquals("Unsupported class must return referral displayName", "Not sure - ask an extension officer", eval.displayName)
            assertFalse("Severity must not be shown for unsupported class", RulesEngine.shouldShowSeverity(eval.certaintyBand))
        }
    }

    @Test
    fun testSupportedClassesAreDiagnosedCorrectly() {
        // Test Rice Bacterial Leaf Blight (index 0) with 92% confidence
        val probsRice = FloatArray(20) { 0.003f }
        probsRice[0] = 0.92f

        val evalRice = VisionMath.evaluateDiagnosis(
            labels = all20Labels,
            probabilities = probsRice,
            thresholds = emptyMap(),
            defaultThreshold = 0.4018f,
            freeEnergy = -5.0,
            energyThreshold = null,
            unsupportedClasses = unsupportedClassIds
        )

        assertEquals("Supported rice class must be diagnosed", "rice_bacterial_leaf_blight", evalRice.classId)
        assertEquals("Supported rice class certainty must be SURE", CertaintyBand.SURE, evalRice.certaintyBand)

        // Test Tomato Yellow Leaf Curl Virus (index 12) with 88% confidence
        val probsTomato = FloatArray(20) { 0.005f }
        probsTomato[12] = 0.88f

        val evalTomato = VisionMath.evaluateDiagnosis(
            labels = all20Labels,
            probabilities = probsTomato,
            thresholds = emptyMap(),
            defaultThreshold = 0.4018f,
            freeEnergy = -5.0,
            energyThreshold = null,
            unsupportedClasses = unsupportedClassIds
        )

        assertEquals("Supported tomato class must be diagnosed", "tomato_yellow_leaf_curl_virus", evalTomato.classId)
        assertEquals("Supported tomato class certainty must be SURE", CertaintyBand.SURE, evalTomato.certaintyBand)
    }
}
