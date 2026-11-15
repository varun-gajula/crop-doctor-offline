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

        // Case finding fields
        assertTrue("Prompt must include Crop", prompt.contains("- Crop: maize"))
        assertTrue("Prompt must include Problem", prompt.contains("- Problem: ${maizeEntry.names.en}"))
        assertTrue("Prompt must include Type", prompt.contains("- Type: disease"))
        assertTrue("Prompt must include Severity", prompt.contains("- Severity: MODERATE"))
        assertTrue("Prompt must include Certainty", prompt.contains("- Confidence Level: Probably"))
        assertTrue("Prompt must include Field Spread", prompt.contains("- Field Spread: some parts of the field"))
        assertTrue("Prompt must include Region", prompt.contains("- Region: Telangana"))
        assertTrue("Prompt must include Urgency", prompt.contains("- Urgency Level: Act This Week"))

        // Approved KB entry contents
        assertTrue("Prompt must include Immediate Step", prompt.contains("- Immediate Cultural Step:"))
        assertTrue("Prompt must include Natural Methods", prompt.contains("- Natural / Cultural Methods:"))
        assertTrue("Prompt must include Chemical Management", prompt.contains("- Chemical Management Options"))
        assertTrue("Prompt must include Regional Guidance", prompt.contains("- Regional Guidance (Telangana):"))
        assertTrue("Prompt must include Referral Rule", prompt.contains("- Official Referral Rule:"))

        // Required instructions
        assertTrue("Prompt must instruct plain words for smallholder farmer", prompt.contains("Explain in plain words for a smallholder farmer"))
        assertTrue("Prompt must instruct to use only steps in KB", prompt.contains("Use only the steps in the knowledge-base entry below"))
        assertTrue("Prompt must instruct urgency ordering", prompt.contains("Choose and order the ones relevant to this urgency"))
        assertTrue("Prompt must instruct what to watch in coming days", prompt.contains("Mention what to watch in the coming days"))
        assertTrue("Prompt must forbid dosage rates", prompt.contains("Do not give doses"))
        assertTrue("Prompt must forbid unlisted products", prompt.contains("Do not name any product not in the entry"))
        assertTrue("Prompt must forbid changing severity/referral", prompt.contains("Do not change the severity or referral advice"))

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

        assertTrue("Pest prompt must state Type: pest", prompt.contains("- Type: pest"))
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
}
