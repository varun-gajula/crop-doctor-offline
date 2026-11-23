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

    private val all15Labels = listOf(
        "rice_leaf_blast",                  // 0
        "rice_healthy",                     // 1
        "cotton_bacterial_blight",          // 2
        "cotton_healthy",                   // 3
        "chilli_leaf_curl_virus",           // 4
        "chilli_healthy",                   // 5
        "maize_northern_leaf_blight",       // 6
        "maize_common_rust",                // 7
        "maize_healthy",                    // 8
        "maize_fall_armyworm",              // 9
        "rice_stem_borer",                  // 10
        "rice_hispa",                       // 11
        "tomato_healthy",                   // 12
        "tomato_septoria_leaf_spot",        // 13
        "other_not_a_crop"                  // 14 (unsupported)
    )

    private val unsupportedClassIds = setOf(
        "other_not_a_crop"
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

        assertEquals("Must have exactly 15 classes", 15, classesObj.size)

        for (i in 0 until 15) {
            val entry = classesObj[i.toString()]!!.jsonObject
            val classId = entry["class_id"]!!.jsonPrimitive.content
            assertEquals("Index $i must match all15Labels[$i]", all15Labels[i], classId)

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
            val idx = all15Labels.indexOf(unsupportedClass)
            assertTrue("Class $unsupportedClass must be present in labels", idx >= 0)

            val probs = FloatArray(15) { 0.002f }
            probs[idx] = 0.95f

            val eval = VisionMath.evaluateDiagnosis(
                labels = all15Labels,
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
        // Test Rice Leaf Blast (index 0) with 92% confidence
        val probsRice = FloatArray(15) { 0.003f }
        probsRice[0] = 0.92f

        val evalRice = VisionMath.evaluateDiagnosis(
            labels = all15Labels,
            probabilities = probsRice,
            thresholds = emptyMap(),
            defaultThreshold = 0.4018f,
            freeEnergy = -5.0,
            energyThreshold = null,
            unsupportedClasses = unsupportedClassIds
        )

        assertEquals("Supported rice class must be diagnosed", "rice_leaf_blast", evalRice.classId)
        assertEquals("Supported rice class certainty must be SURE", CertaintyBand.SURE, evalRice.certaintyBand)

        // Test Tomato Septoria Leaf Spot (index 13) with 88% confidence
        val probsTomato = FloatArray(15) { 0.005f }
        probsTomato[13] = 0.88f

        val evalTomato = VisionMath.evaluateDiagnosis(
            labels = all15Labels,
            probabilities = probsTomato,
            thresholds = emptyMap(),
            defaultThreshold = 0.4018f,
            freeEnergy = -5.0,
            energyThreshold = null,
            unsupportedClasses = unsupportedClassIds
        )

        assertEquals("Supported tomato class must be diagnosed", "tomato_septoria_leaf_spot", evalTomato.classId)
        assertEquals("Supported tomato class certainty must be SURE", CertaintyBand.SURE, evalTomato.certaintyBand)
    }
}
