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
 * Task V4 Unit Tests: Verification of Dropped Classes & 21-Class Architecture
 * 1. Confirms the 4 dropped classes (cotton_leaf_curl_virus, cotton_alternaria_leaf_spot,
 *    chilli_cercospora_leaf_spot, chilli_white_spot) are completely absent from supported_classes.json,
 *    labels.json, and thresholds.json.
 * 2. Confirms supported_classes.json has exactly 21 classes (indices 0..20), all supported=true.
 * 3. Confirms removed text is safely archived in docs/removed_classes/.
 * 4. Confirms VisionMath safety: if any class is marked unsupported, it returns "Can't tell" (unknown / NOT_SURE).
 */
class BlockedClassesHonestyTest {

    private val all21Labels = listOf(
        "rice_bacterial_leaf_blight",       // 0
        "rice_brown_spot",                  // 1
        "rice_leaf_blast",                  // 2
        "rice_sheath_blight",               // 3
        "rice_healthy",                     // 4
        "cotton_bacterial_blight",          // 5
        "cotton_fusarium_wilt",             // 6
        "cotton_healthy",                   // 7
        "chilli_bacterial_spot",            // 8
        "chilli_leaf_curl_virus",           // 9
        "chilli_healthy",                   // 10
        "tomato_early_blight",              // 11
        "tomato_late_blight",               // 12
        "tomato_yellow_leaf_curl_virus",    // 13
        "tomato_spider_mites",              // 14
        "tomato_healthy",                   // 15
        "maize_gray_leaf_spot",             // 16
        "maize_common_rust",                // 17
        "maize_northern_leaf_blight",       // 18
        "maize_fall_armyworm",              // 19
        "maize_healthy"                     // 20
    )

    private val droppedClassIds = setOf(
        "cotton_leaf_curl_virus",
        "cotton_alternaria_leaf_spot",
        "chilli_cercospora_leaf_spot",
        "chilli_white_spot"
    )

    private val thresholds = mapOf(
        "rice_bacterial_leaf_blight" to 0.40f,
        "cotton_bacterial_blight" to 0.40f,
        "tomato_early_blight" to 0.40f
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
    fun testSupportedClassesJsonHasExactly21ClassesAllSupported() {
        val target = findFile(listOf(
            "app/src/main/assets/supported_classes.json",
            "src/main/assets/supported_classes.json",
            "data/supported_classes.json"
        ))

        val json = Json.parseToJsonElement(target.readText()).jsonObject
        val classesObj = json["classes"]!!.jsonObject

        assertEquals("Must have exactly 21 classes", 21, classesObj.size)

        for (i in 0 until 21) {
            val entry = classesObj[i.toString()]!!.jsonObject
            val classId = entry["class_id"]!!.jsonPrimitive.content
            assertEquals("Index $i must match all21Labels[$i]", all21Labels[i], classId)
            assertTrue("Class $classId must have supported=true", entry["supported"]!!.jsonPrimitive.boolean)
        }
    }

    @Test
    fun testDroppedClassesAreCompletelyAbsentFromAssets() {
        val supportedFile = findFile(listOf(
            "app/src/main/assets/supported_classes.json",
            "src/main/assets/supported_classes.json",
            "data/supported_classes.json"
        ))
        val supportedText = supportedFile.readText()

        val labelsFile = findFile(listOf(
            "app/src/main/assets/labels.json",
            "src/main/assets/labels.json"
        ))
        val labelsText = labelsFile.readText()

        val thresholdsFile = findFile(listOf(
            "app/src/main/assets/thresholds.json",
            "src/main/assets/thresholds.json",
            "ml/thresholds.json"
        ))
        val thresholdsText = thresholdsFile.readText()

        val kbFile = findFile(listOf(
            "app/src/main/assets/kb/knowledge_base.json",
            "src/main/assets/kb/knowledge_base.json",
            "data/kb/knowledge_base.json"
        ))
        val kbText = kbFile.readText()

        for (dropped in droppedClassIds) {
            assertFalse("dropped class $dropped must NOT be in supported_classes.json", supportedText.contains("\"$dropped\""))
            assertFalse("dropped class $dropped must NOT be in labels.json", labelsText.contains("\"$dropped\""))
            assertFalse("dropped class $dropped must NOT be in thresholds.json", thresholdsText.contains("\"$dropped\""))
            assertFalse("dropped class $dropped must NOT be in knowledge_base.json", kbText.contains("\"$dropped\""))
        }
    }

    @Test
    fun testRemovedClassesAreArchivedInDocs() {
        val archiveFile = findFile(listOf(
            "docs/removed_classes/removed_kb_entries.json",
            "../docs/removed_classes/removed_kb_entries.json"
        ))
        assertTrue("Archive file must exist", archiveFile.exists())
        val archiveText = archiveFile.readText()
        for (dropped in droppedClassIds) {
            assertTrue("Archive must preserve $dropped", archiveText.contains("\"$dropped\""))
        }
    }

    @Test
    fun testSimulatedUnsupportedClassIsTreatedAsCantTell() {
        // If an unsupported class set is passed to evaluateDiagnosis, it must return "Can't tell"
        val syntheticUnsupported = setOf("cotton_leaf_curl_virus", "synthetic_test_class")

        val probs = FloatArray(21) { 0.002f }
        probs[0] = 0.95f // Top prediction is index 0

        // Simulate index 0 being in unsupportedClasses
        val eval = VisionMath.evaluateDiagnosis(
            labels = all21Labels,
            probabilities = probs,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = -5.0,
            energyThreshold = -1.5,
            unsupportedClasses = setOf(all21Labels[0])
        )

        assertEquals("Blocked class must return 'unknown'", "unknown", eval.classId)
        assertEquals("Blocked class must return NOT_SURE certainty", CertaintyBand.NOT_SURE, eval.certaintyBand)
        assertEquals("Blocked class must return referral displayName", "Not sure - ask an extension officer", eval.displayName)
        assertFalse("Severity must not be shown for unsupported class", RulesEngine.shouldShowSeverity(eval.certaintyBand))
    }

    @Test
    fun testSupportedClassesAreDiagnosedCorrectly() {
        // Test Rice Bacterial Leaf Blight (index 0) with 92% confidence
        val probsRice = FloatArray(21) { 0.003f }
        probsRice[0] = 0.92f

        val evalRice = VisionMath.evaluateDiagnosis(
            labels = all21Labels,
            probabilities = probsRice,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = -5.0,
            energyThreshold = -1.5,
            unsupportedClasses = emptySet()
        )

        assertEquals("Supported rice class must be diagnosed", "rice_bacterial_leaf_blight", evalRice.classId)
        assertEquals("Supported rice class certainty must be SURE", CertaintyBand.SURE, evalRice.certaintyBand)

        // Test Tomato Early Blight (index 11) with 88% confidence
        val probsTomato = FloatArray(21) { 0.005f }
        probsTomato[11] = 0.88f

        val evalTomato = VisionMath.evaluateDiagnosis(
            labels = all21Labels,
            probabilities = probsTomato,
            thresholds = thresholds,
            defaultThreshold = 0.40f,
            freeEnergy = -5.0,
            energyThreshold = -1.5,
            unsupportedClasses = emptySet()
        )

        assertEquals("Supported tomato class must be diagnosed", "tomato_early_blight", evalTomato.classId)
        assertEquals("Supported tomato class certainty must be SURE", CertaintyBand.SURE, evalTomato.certaintyBand)
    }
}
