package com.cropdoctor

import com.cropdoctor.advisor.PromptBuilder
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBaseRoot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Unit tests verifying Requirement F3 and Rule G1:
 * 1. Every class has a type ('disease', 'pest', or 'healthy').
 * 2. Pest results never show a leaf-disease severity sentence that does not apply.
 * 3. Pest entries contain official pest-management steps (cultural/biological first, chemical last).
 * 4. Grounded prompts for pests describe pest infestation/damage rather than leaf-disease lesion area.
 */
class PestVsDiseaseTest {

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private lateinit var kbRoot: KnowledgeBaseRoot
        private lateinit var kbFile: File
        private lateinit var supportedClassesFile: File

        @BeforeClass
        @JvmStatic
        fun setUp() {
            kbFile = File("src/main/assets/kb/knowledge_base.json")
            if (!kbFile.exists()) {
                kbFile = File("data/kb/knowledge_base.json")
            }
            assertTrue("Knowledge Base JSON must exist", kbFile.exists())
            val content = kbFile.readText()
            kbRoot = json.decodeFromString(content)

            supportedClassesFile = File("src/main/assets/supported_classes.json")
            if (!supportedClassesFile.exists()) {
                supportedClassesFile = File("data/supported_classes.json")
            }
            assertTrue("supported_classes.json must exist", supportedClassesFile.exists())
        }
    }

    @Test
    fun testEveryClassHasValidType() {
        assertEquals("Expected exactly 25 classes in knowledge base", 25, kbRoot.classes.size)
        val validTypes = setOf("disease", "pest", "healthy")

        for (entry in kbRoot.classes) {
            assertNotNull("${entry.classId}: type must not be null", entry.type)
            assertTrue(
                "${entry.classId}: type '${entry.type}' must be one of $validTypes",
                validTypes.contains(entry.type.lowercase())
            )
        }
    }

    @Test
    fun testExpectedClassTypeDistribution() {
        val pestClasses = kbRoot.classes.filter { it.type == "pest" }.map { it.classId }.toSet()
        val healthyClasses = kbRoot.classes.filter { it.type == "healthy" }.map { it.classId }.toSet()
        val diseaseClasses = kbRoot.classes.filter { it.type == "disease" }.map { it.classId }.toSet()

        assertEquals("Expected exactly 2 pest classes", 2, pestClasses.size)
        assertTrue("Expected tomato_spider_mites in pest classes", pestClasses.contains("tomato_spider_mites"))
        assertTrue("Expected maize_fall_armyworm in pest classes", pestClasses.contains("maize_fall_armyworm"))

        assertEquals("Expected exactly 5 healthy classes", 5, healthyClasses.size)
        assertEquals("Expected exactly 18 disease classes", 18, diseaseClasses.size)
    }

    @Test
    fun testSupportedClassesFileHasMatchingTypes() {
        val jsonStr = supportedClassesFile.readText()
        val rootObj = Json.parseToJsonElement(jsonStr).jsonObject
        val classesObj = rootObj["classes"]!!.jsonObject

        for (entry in kbRoot.classes) {
            val key = entry.index.toString()
            assertTrue("supported_classes.json must contain index $key", classesObj.containsKey(key))
            val classObj = classesObj[key]!!.jsonObject
            assertEquals("Class ID mismatch at index $key", entry.classId, classObj["class_id"]!!.jsonPrimitive.content)
            assertTrue("Class at index $key must have 'type' field", classObj.containsKey("type"))
            val scType = classObj["type"]!!.jsonPrimitive.content
            assertEquals(
                "Type mismatch for ${entry.classId} between KB and supported_classes.json",
                entry.type,
                scType
            )
        }
    }

    @Test
    fun testPestSeverityActionSentenceNeverShowsLeafDiseaseSentence() {
        // Disease action sentences
        val diseaseMildRes = RulesEngine.getSeverityActionSentence(SeverityBand.MILD, "disease")
        val diseaseModerateRes = RulesEngine.getSeverityActionSentence(SeverityBand.MODERATE, "disease")
        val diseaseSevereRes = RulesEngine.getSeverityActionSentence(SeverityBand.SEVERE, "disease")

        assertEquals(R.string.severity_early_warning, diseaseMildRes)
        assertEquals(R.string.severity_act_now, diseaseModerateRes)
        assertEquals(R.string.severity_act_now, diseaseSevereRes)

        // Pest action sentences
        val pestMildRes = RulesEngine.getSeverityActionSentence(SeverityBand.MILD, "pest")
        val pestModerateRes = RulesEngine.getSeverityActionSentence(SeverityBand.MODERATE, "pest")
        val pestSevereRes = RulesEngine.getSeverityActionSentence(SeverityBand.SEVERE, "pest")

        assertEquals(R.string.severity_pest_early_warning, pestMildRes)
        assertEquals(R.string.severity_pest_act_now, pestModerateRes)
        assertEquals(R.string.severity_pest_act_now, pestSevereRes)

        // Pest severity sentences must NEVER match leaf-disease severity sentences
        assertNotEquals(
            "Pest mild severity sentence must not be the leaf-disease sentence",
            diseaseMildRes,
            pestMildRes
        )
        assertNotEquals(
            "Pest moderate/severe action sentence must not be the leaf-disease sentence",
            diseaseModerateRes,
            pestModerateRes
        )
        assertNotEquals(
            "Pest severe action sentence must not be the leaf-disease sentence",
            diseaseSevereRes,
            pestSevereRes
        )
    }

    @Test
    fun testPestGuidanceSentenceNeverShowsLeafDiseaseSentence() {
        val guidanceMildPest = RulesEngine.getGuidanceSentence(CertaintyBand.SURE, SeverityBand.MILD, "pest")
        val guidanceModeratePest = RulesEngine.getGuidanceSentence(CertaintyBand.SURE, SeverityBand.MODERATE, "pest")
        val guidanceSeverePest = RulesEngine.getGuidanceSentence(CertaintyBand.SURE, SeverityBand.SEVERE, "pest")

        assertEquals(R.string.severity_pest_early_warning, guidanceMildPest)
        assertEquals(R.string.severity_pest_act_now, guidanceModeratePest)
        assertEquals(R.string.severity_pest_act_now, guidanceSeverePest)

        assertNotEquals(R.string.severity_early_warning, guidanceMildPest)
        assertNotEquals(R.string.severity_act_now, guidanceModeratePest)
    }

    @Test
    fun testPestEntriesHaveRequiredPestManagementSteps() {
        val pestEntries = kbRoot.classes.filter { it.type == "pest" }
        assertEquals(2, pestEntries.size)

        for (pest in pestEntries) {
            assertTrue("${pest.classId}: needs_expert_review must be true", pest.needsExpertReview)

            // Must have biological and cultural control steps first
            val naturalSteps = pest.treatment.natural
            assertTrue("${pest.classId}: must have at least 2 natural/IPM steps", naturalSteps.size >= 2)

            // Check official citations (ICAR, NBAIR, NIPHM, PJTSAU, ANGRAU)
            for (step in naturalSteps) {
                assertFalse("${pest.classId}: action must not be empty", step.action.en.isBlank())
                assertFalse("${pest.classId}: source must not be empty", step.source.isBlank())
                assertTrue(
                    "${pest.classId}: source must cite official authority",
                    step.source.contains("NBAIR") || step.source.contains("NIPHM") ||
                            step.source.contains("ICAR") || step.source.contains("PJTSAU") ||
                            step.source.contains("ANGRAU") || step.source.contains("IIHR")
                )
            }

            // Chemical steps (chemical last)
            val chemicalSteps = pest.treatment.chemical
            assertTrue("${pest.classId}: must have chemical options", chemicalSteps.isNotEmpty())
            for (chem in chemicalSteps) {
                assertFalse("${pest.classId}: dose_verified must be false until signed off", chem.doseVerified)
                assertEquals("${pest.classId}: unverified dose must be 'follow the product label'", "follow the product label", chem.dose)
                assertTrue("${pest.classId}: chemical source must be CIB&RC", chem.source.contains("CIB&RC"))
            }
        }
    }

    @Test
    fun testPromptBuilderForPestsUsesPestInfestationContext() {
        val spiderMites = kbRoot.classes.first { it.classId == "tomato_spider_mites" }
        val prompt = PromptBuilder.buildPrompt(
            crop = spiderMites.crop,
            diseaseName = spiderMites.names.en,
            severity = "MODERATE",
            certainty = "Sure",
            kbEntry = spiderMites,
            language = "en",
            severityPercent = 18f,
            confidenceScore = 91f
        )

        assertTrue("Pest prompt must mention pest type", prompt.contains("(pest)"))
        assertTrue("Pest prompt must mention pest infestation/plant damage", prompt.contains("pest infestation / plant damage"))
        assertFalse("Pest prompt must NOT contain 'affected leaf area'", prompt.contains("affected leaf area"))
    }

    @Test
    fun testPromptBuilderForDiseasesUsesAffectedLeafAreaContext() {
        val earlyBlight = kbRoot.classes.first { it.classId == "tomato_early_blight" }
        val prompt = PromptBuilder.buildPrompt(
            crop = earlyBlight.crop,
            diseaseName = earlyBlight.names.en,
            severity = "MODERATE",
            certainty = "Sure",
            kbEntry = earlyBlight,
            language = "en",
            severityPercent = 22f,
            confidenceScore = 88f
        )

        assertTrue("Disease prompt must mention disease type", prompt.contains("(disease)"))
        assertTrue("Disease prompt must contain 'affected leaf area'", prompt.contains("affected leaf area"))
        assertFalse("Disease prompt must NOT contain 'pest infestation'", prompt.contains("pest infestation"))
    }
}
