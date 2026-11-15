package com.cropdoctor.advisor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KnowledgeBaseRoot
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Task A2 Requirement 5 Unit Tests:
 * 1. Build the Telugu answer in code from reviewed sentence pieces chosen by the case.
 * 2. Two different cases produce different Telugu text.
 * 3. Telugu never reaches Llama (PromptBuilder throws IllegalArgumentException, Advisor routes to offline builder).
 */
class TeluguAdvisoryTest {

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private lateinit var kbRoot: KnowledgeBaseRoot

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val candidates = listOf(
                File("src/main/assets/kb/knowledge_base.json"),
                File("data/kb/knowledge_base.json"),
                File("../data/kb/knowledge_base.json")
            )
            val kbFile = candidates.firstOrNull { it.exists() }
                ?: error("Could not find knowledge_base.json")
            kbRoot = json.decodeFromString(kbFile.readText())
        }
    }

    @Test
    fun testTwoDifferentCasesProduceDifferentTeluguText() {
        val maizeEntry = kbRoot.classes.first { it.classId == "maize_common_rust" }
        val tomatoEntry = kbRoot.classes.first { it.classId == "tomato_early_blight" }

        // Case 1: Maize Common Rust, Moderate, Some plants, Act now
        val case1Telugu = RulesEngine.buildCaseSpecificAdvisory(
            classId = maizeEntry.classId,
            crop = maizeEntry.crop,
            diseaseName = maizeEntry.names.te,
            type = maizeEntry.type,
            severityBand = SeverityBand.MODERATE,
            certaintyBand = CertaintyBand.SURE,
            fieldSpread = RulesEngine.FieldSpread.SOME,
            urgencyLevel = RulesEngine.UrgencyLevel.ACT_NOW,
            region = "telangana",
            language = "te",
            kbEntry = maizeEntry
        )

        // Case 2: Tomato Early Blight, Mild, Few plants, Early warning
        val case2Telugu = RulesEngine.buildCaseSpecificAdvisory(
            classId = tomatoEntry.classId,
            crop = tomatoEntry.crop,
            diseaseName = tomatoEntry.names.te,
            type = tomatoEntry.type,
            severityBand = SeverityBand.MILD,
            certaintyBand = CertaintyBand.SURE,
            fieldSpread = RulesEngine.FieldSpread.FEW_PLANTS,
            urgencyLevel = RulesEngine.UrgencyLevel.EARLY_WARNING,
            region = "andhra_pradesh",
            language = "te",
            kbEntry = tomatoEntry
        )

        assertNotNull(case1Telugu)
        assertNotNull(case2Telugu)
        assertTrue("Telugu text 1 must not be blank", case1Telugu.isNotBlank())
        assertTrue("Telugu text 2 must not be blank", case2Telugu.isNotBlank())

        // 1. Assert they produce different Telugu text
        assertNotEquals("Different cases must produce different Telugu text", case1Telugu, case2Telugu)

        // 2. Assert Case 1 has maize terms and Act Now urgency
        assertTrue("Case 1 must contain Telugu crop name 'మొక్కజొన్న'", case1Telugu.contains("మొక్కజొన్న"))
        assertTrue("Case 1 must contain Telugu severity 'మధ్యస్థ తీవ్రత'", case1Telugu.contains("మధ్యస్థ తీవ్రత"))
        assertTrue("Case 1 must contain Telugu urgency 'తక్షణ చర్య అవసరం'", case1Telugu.contains("తక్షణ చర్య అవసరం"))
        assertTrue("Case 1 must contain immediate referral phone", case1Telugu.contains("1800-180-1551"))
        assertFalse("Case 1 must NOT contain tomato name", case1Telugu.contains("టమాటా"))

        // 3. Assert Case 2 has tomato terms and Early Warning urgency
        assertTrue("Case 2 must contain Telugu crop name 'టమాటా'", case2Telugu.contains("టమాటా"))
        assertTrue("Case 2 must contain Telugu severity 'ప్రారంభ దశ'", case2Telugu.contains("ప్రారంభ దశ"))
        assertTrue("Case 2 must contain Telugu urgency 'ప్రారంభ హెచ్చరిక'", case2Telugu.contains("ప్రారంభ హెచ్చరిక"))
        assertFalse("Case 2 must NOT contain maize name", case2Telugu.contains("మొక్కజొన్న"))

        // 4. Assert both contain the standard three-part Telugu structure
        for (text in listOf(case1Telugu, case2Telugu)) {
            assertTrue("Must contain section 1", text.contains("1. నేను గమనించినది:"))
            assertTrue("Must contain section 2", text.contains("2. ఇప్పుడు చేయవలసిన పనులు:"))
            assertTrue("Must contain section 3", text.contains("3. ఎప్పుడు సహాయం పొందాలి:"))
        }
    }

    @Test
    fun testPestVsDiseaseProducesDifferentTeluguTypes() {
        val pestEntry = kbRoot.classes.first { it.classId == "tomato_spider_mites" }
        val diseaseEntry = kbRoot.classes.first { it.classId == "tomato_early_blight" }

        val pestTelugu = RulesEngine.buildCaseSpecificAdvisory(
            classId = pestEntry.classId,
            crop = pestEntry.crop,
            diseaseName = pestEntry.names.te,
            type = pestEntry.type,
            severityBand = SeverityBand.MILD,
            certaintyBand = CertaintyBand.SURE,
            fieldSpread = RulesEngine.FieldSpread.FEW_PLANTS,
            urgencyLevel = RulesEngine.UrgencyLevel.EARLY_WARNING,
            region = "telangana",
            language = "te",
            kbEntry = pestEntry
        )

        val diseaseTelugu = RulesEngine.buildCaseSpecificAdvisory(
            classId = diseaseEntry.classId,
            crop = diseaseEntry.crop,
            diseaseName = diseaseEntry.names.te,
            type = diseaseEntry.type,
            severityBand = SeverityBand.MILD,
            certaintyBand = CertaintyBand.SURE,
            fieldSpread = RulesEngine.FieldSpread.FEW_PLANTS,
            urgencyLevel = RulesEngine.UrgencyLevel.EARLY_WARNING,
            region = "telangana",
            language = "te",
            kbEntry = diseaseEntry
        )

        assertTrue("Pest Telugu text must contain 'పురుగు'", pestTelugu.contains("పురుగు"))
        assertTrue("Disease Telugu text must contain 'తెగులు'", diseaseTelugu.contains("తెగులు"))
        assertNotEquals(pestTelugu, diseaseTelugu)
    }

    @Test
    fun testTeluguNeverReachesLlamaPromptBuilder() {
        val entry = kbRoot.classes.first()
        try {
            PromptBuilder.buildPrompt(
                crop = entry.crop,
                diseaseName = entry.names.te,
                severity = "MODERATE",
                certainty = "Sure",
                kbEntry = entry,
                language = "te"
            )
            fail("PromptBuilder must throw IllegalArgumentException when language is 'te' (Rule H6)")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Exception message must mention Rule H6 or Telugu prohibition: ${e.message}",
                e.message!!.contains("Rule H6") || e.message!!.contains("Telugu")
            )
        }
    }
}
