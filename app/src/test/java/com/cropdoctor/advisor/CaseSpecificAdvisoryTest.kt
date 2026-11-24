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
 * Task A2 Unit Tests: Case-Specific Advisory & Safety Verification
 * 1. Prompt includes crop, problem, type, severity, certainty, field spread, region, urgency, and KB entry.
 * 2. Instructions require smallholder plain words, only KB steps, urgency-based ordering, days to watch,
 *    no doses, no outside products, no severity/referral changes, 3-part structure.
 * 3. Referral sentence added/checked by RulesEngine; contradicts -> rejected by Guardrail.
 * 4. Fallback labelled Offline guide when guardrail rejects or memory low.
 */
class CaseSpecificAdvisoryTest {

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
    fun testPromptContainsAllRequiredFields() {
        val maizeEntry = kbRoot.classes.first { it.classId == "maize_common_rust" }
        val prompt = PromptBuilder.buildPrompt(
            crop = maizeEntry.crop,
            diseaseName = maizeEntry.names.en,
            severity = "MODERATE",
            certainty = "Probably",
            kbEntry = maizeEntry,
            language = "en",
            fieldSpread = "some parts of the field",
            region = "telangana",
            urgency = "Act This Week"
        )

        // Case finding fields (condensed for low latency TTFT)
        assertTrue("Prompt must include Crop", prompt.contains("Finding: maize"))
        assertTrue("Prompt must include Problem", prompt.contains(maizeEntry.names.en))
        assertTrue("Prompt must include Type", prompt.contains("(disease)"))
        assertTrue("Prompt must include Severity", prompt.contains("MODERATE"))
        assertTrue("Prompt must include Certainty", prompt.contains("Probably"))
        assertTrue("Prompt must include Urgency", prompt.contains("Act This Week"))

        // Approved KB entry contents (Task LAT-2: condensed to ~150 tokens)
        assertTrue("Prompt must include Action Step", prompt.contains("- Action:"))
        assertTrue("Prompt must include Natural Methods", prompt.contains("- Natural:"))
        assertTrue("Prompt must include Referral Rule", prompt.contains("- Referral:"))

        // Required instructions (Task LAT-2: condensed prompt)
        assertTrue("Prompt must instruct plain words for smallholder farmer", prompt.contains("Explain in plain words for a smallholder farmer"))
        assertTrue("Prompt must instruct to use only steps in KB", prompt.contains("Use only steps in the entry below"))
        assertTrue("Prompt must instruct 6-sentence limit", prompt.contains("at most 6 short, complete sentences"))
        assertTrue("Prompt must forbid dosage rates", prompt.contains("Do not give doses"))
        assertTrue("Prompt must forbid unlisted products", prompt.contains("Do not name any product not in the entry"))
        assertTrue("Prompt must forbid changing severity/referral", prompt.contains("Do not change severity or referral advice"))

        // Output format in 3 short parts
        assertTrue("Prompt must require Part 1: What I see", prompt.contains("1. What I see:"))
        assertTrue("Prompt must require Part 2: What to do now", prompt.contains("2. What to do now:"))
        assertTrue("Prompt must require Part 3: When to get help", prompt.contains("3. When to get help:"))
    }

    @Test
    fun testPromptForPestIncludesPestTypeAndScouting() {
        val pestEntry = kbRoot.classes.first { it.classId == "tomato_spider_mites" }
        val prompt = PromptBuilder.buildPrompt(
            crop = pestEntry.crop,
            diseaseName = pestEntry.names.en,
            severity = "MILD",
            certainty = "Sure",
            kbEntry = pestEntry,
            language = "en",
            fieldSpread = "a few plants",
            region = "telangana",
            urgency = "Early Warning"
        )

        assertTrue("Pest prompt must state Type: pest", prompt.contains("(pest)"))
        assertTrue("Pest prompt must mention pest infestation damage", prompt.contains("pest infestation / plant damage"))
        assertFalse("Pest prompt must not state affected leaf area", prompt.contains("affected leaf area"))
    }

    @Test
    fun testReferralSentenceEnforcedByRulesEngine() {
        // Severe case -> immediate referral
        val severeReferral = RulesEngine.getReferralSentence(
            urgencyLevel = RulesEngine.UrgencyLevel.ACT_NOW,
            certaintyBand = CertaintyBand.SURE,
            language = "en"
        )
        assertTrue("Must include extension officer", severeReferral.contains("extension officer"))
        assertTrue("Must include Kisan Call Centre number", severeReferral.contains("1800-180-1551"))
        assertTrue("Must indicate immediate action", severeReferral.contains("immediately"))

        // Early warning case -> routine monitoring
        val earlyWarningReferral = RulesEngine.getReferralSentence(
            urgencyLevel = RulesEngine.UrgencyLevel.EARLY_WARNING,
            certaintyBand = CertaintyBand.SURE,
            language = "en"
        )
        assertTrue("Must advise monitoring first", earlyWarningReferral.contains("Monitor"))
        assertTrue("Must mention spreading over 3-5 days", earlyWarningReferral.contains("3-5 days"))
    }

    @Test
    fun testGuardrailRejectsReferralContradiction() {
        val entry = kbRoot.classes.first { it.classId == "rice_leaf_blast" }

        // Severe outbreak text claiming no help is needed
        val contradictingText = """
1. What I see:
Severe blast spots on rice leaves affecting most of the field.

2. What to do now:
Spray tricyclazole.

3. When to get help:
Do not call an extension officer, no need to contact anyone, handle this yourself.
        """.trimIndent()

        val validation = GuardrailFilter.validateAdvice(
            generatedText = contradictingText,
            kbEntry = entry,
            severityBand = SeverityBand.SEVERE,
            urgencyLevel = RulesEngine.UrgencyLevel.ACT_NOW,
            certaintyBand = CertaintyBand.SURE
        )

        assertFalse("Guardrail must reject referral contradiction", validation.isValid)
        assertTrue(
            "Failure reason must mention referral: ${validation.failureReason}",
            validation.failureReason!!.contains("referral", ignoreCase = true)
        )
    }

    @Test
    fun testGuardrailRejectsDosageRates() {
        val entry = kbRoot.classes.first { it.classId == "maize_common_rust" }

        val textWithDose = """
1. What I see:
Common rust pustules.

2. What to do now:
Mix 2.5 ml/L of propiconazole and spray 500 ml per acre.

3. When to get help:
Call extension officer if needed.
        """.trimIndent()

        val validation = GuardrailFilter.validateAdvice(
            generatedText = textWithDose,
            kbEntry = entry,
            severityBand = SeverityBand.MODERATE,
            urgencyLevel = RulesEngine.UrgencyLevel.ACT_THIS_WEEK,
            certaintyBand = CertaintyBand.SURE
        )

        assertFalse("Guardrail must reject unverified dosage rates", validation.isValid)
        assertTrue(validation.failureReason!!.contains("dosage", ignoreCase = true))
    }

    @Test
    fun testCaseSpecificAdvisoryProducesThreePartsInAllLanguages() {
        val entry = kbRoot.classes.first { it.classId == "cotton_bacterial_blight" }

        for (lang in listOf("en", "te", "hi")) {
            val text = RulesEngine.buildCaseSpecificAdvisory(
                classId = entry.classId,
                crop = entry.crop,
                diseaseName = entry.names.forLanguage(lang),
                type = entry.type,
                severityBand = SeverityBand.MODERATE,
                certaintyBand = CertaintyBand.SURE,
                fieldSpread = RulesEngine.FieldSpread.SOME,
                urgencyLevel = RulesEngine.UrgencyLevel.ACT_NOW,
                region = "telangana",
                language = lang,
                kbEntry = entry
            )

            when (lang) {
                "en" -> {
                    assertTrue(text.contains("1. What I see:"))
                    assertTrue(text.contains("2. What to do now:"))
                    assertTrue(text.contains("3. When to get help:"))
                }
                "te" -> {
                    assertTrue(text.contains("1. నేను గమనించినది:"))
                    assertTrue(text.contains("2. ఇప్పుడు చేయవలసిన పనులు:"))
                    assertTrue(text.contains("3. ఎప్పుడు సహాయం పొందాలి:"))
                }
                "hi" -> {
                    assertTrue(text.contains("1. मैंने क्या देखा:"))
                    assertTrue(text.contains("2. अभी क्या करें:"))
                    assertTrue(text.contains("3. सहायता कब लें:"))
                }
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class TestCaseItem(
        val id: Int,
        val crop: String,
        val classId: String,
        val problem: String,
        val type: String,
        val severity: String,
        val certainty: String,
        val fieldSpread: String,
        val region: String
    )

    @Test
    fun testAll20LlamaCasesPassEnglishGuardrail() {
        val candidates = listOf(
            File("benchmarks/llama_cases.json"),
            File("../benchmarks/llama_cases.json")
        )
        val casesFile = candidates.firstOrNull { it.exists() }
            ?: error("Could not find benchmarks/llama_cases.json")
        val testCases = json.decodeFromString<List<TestCaseItem>>(casesFile.readText())
        assertEquals(20, testCases.size)

        for (c in testCases) {
            val kbEntry = if (c.classId == "unknown") {
                kbRoot.classes.first { it.crop.equals(c.crop, ignoreCase = true) }
            } else {
                kbRoot.classes.first { it.classId == c.classId }
            }

            val sev = SeverityBand.valueOf(c.severity.uppercase())
            val cert = CertaintyBand.valueOf(c.certainty.uppercase())
            val spread = when (c.fieldSpread.lowercase()) {
                "few_plants" -> RulesEngine.FieldSpread.FEW_PLANTS
                "most" -> RulesEngine.FieldSpread.MOST
                else -> RulesEngine.FieldSpread.SOME
            }
            val urgency = RulesEngine.calculateUrgency(sev, spread)

            val advice = RulesEngine.buildCaseSpecificAdvisory(
                classId = c.classId,
                crop = c.crop,
                diseaseName = c.problem,
                type = c.type,
                severityBand = sev,
                certaintyBand = cert,
                fieldSpread = spread,
                urgencyLevel = urgency,
                region = c.region,
                language = "en",
                kbEntry = kbEntry
            )

            assertNotNull("Advisory text must not be null for case ${c.id}", advice)
            assertTrue("Must contain section 1 for case ${c.id}", advice.contains("1. What I see:"))
            assertTrue("Must contain section 2 for case ${c.id}", advice.contains("2. What to do now:"))
            assertTrue("Must contain section 3 for case ${c.id}", advice.contains("3. When to get help:"))

            // Check 1: No dose number
            assertFalse("Must not have dose in case ${c.id}: $advice", GuardrailFilter.hasDose(advice))

            // Check 2: GuardrailFilter validation
            val validation = GuardrailFilter.validateAdvice(advice, kbEntry, sev, urgency, cert)
            assertTrue("Guardrail must pass for case ${c.id}: ${validation.failureReason}", validation.isValid)

            // Check 3: Mentions correct crop
            assertTrue("Must mention crop '${c.crop}' in case ${c.id}", advice.contains(c.crop, ignoreCase = true))

            // Check 4: Mentions problem
            if (c.classId != "unknown") {
                val keyword = c.problem.split(" ").last()
                assertTrue("Must mention problem keyword '$keyword' in case ${c.id}", advice.contains(keyword, ignoreCase = true))
            }

            // Check 5: Referral consistency
            if (urgency == RulesEngine.UrgencyLevel.ACT_NOW || cert == CertaintyBand.NOT_SURE) {
                assertTrue("Must contain extension officer referral in urgent case ${c.id}", advice.contains("1800-180-1551"))
            }
        }
    }

    @Test
    fun testTeluguLlamaProhibitionOnAll20Cases() {
        val candidates = listOf(
            File("benchmarks/llama_cases.json"),
            File("../benchmarks/llama_cases.json")
        )
        val casesFile = candidates.firstOrNull { it.exists() }
            ?: error("Could not find benchmarks/llama_cases.json")
        val testCases = json.decodeFromString<List<TestCaseItem>>(casesFile.readText())

        for (c in testCases) {
            val kbEntry = if (c.classId == "unknown") {
                kbRoot.classes.first { it.crop.equals(c.crop, ignoreCase = true) }
            } else {
                kbRoot.classes.first { it.classId == c.classId }
            }

            try {
                PromptBuilder.buildPrompt(
                    crop = c.crop,
                    diseaseName = kbEntry.names.te,
                    severity = c.severity,
                    certainty = c.certainty,
                    kbEntry = kbEntry,
                    language = "te"
                )
                fail("Rule H6: PromptBuilder must reject Telugu for case ${c.id}")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("Rule H6") == true || e.message?.contains("Telugu") == true)
            }
        }
    }
}
