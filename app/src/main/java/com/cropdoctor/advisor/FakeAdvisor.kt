package com.cropdoctor.advisor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import kotlinx.coroutines.delay

/**
 * FakeAdvisor enables UI testing, demonstration, and emulator development.
 * It simulates streaming token generation from Llama 3.2 1B with grounded, safe advice.
 */
class FakeAdvisor : Advisor {

    override val isFake: Boolean = true

    override suspend fun generateAdvice(
        classId: String,
        severityBand: SeverityBand,
        certainty: String,
        language: String,
        kbEntry: KbEntry,
        fieldSpread: String,
        region: String,
        urgency: RulesEngine.UrgencyLevel?,
        crop: String?,
        onToken: (String) -> Unit
    ) {
        val certaintyBand = when (certainty.uppercase()) {
            "SURE" -> CertaintyBand.SURE
            "PROBABLY" -> CertaintyBand.PROBABLY
            else -> CertaintyBand.NOT_SURE
        }

        val spreadEnum = when (fieldSpread.lowercase()) {
            "few_plants", "a few plants", "few" -> RulesEngine.FieldSpread.FEW_PLANTS
            "most" -> RulesEngine.FieldSpread.MOST
            else -> RulesEngine.FieldSpread.SOME
        }

        val resolvedUrgency = urgency ?: RulesEngine.calculateUrgency(severityBand, spreadEnum)
        val effectiveCrop = crop ?: kbEntry.crop

        val adviceText = RulesEngine.buildCaseSpecificAdvisory(
            classId = classId,
            crop = effectiveCrop,
            diseaseName = kbEntry.names.forLanguage(language),
            type = kbEntry.type,
            severityBand = severityBand,
            certaintyBand = certaintyBand,
            fieldSpread = spreadEnum,
            urgencyLevel = resolvedUrgency,
            region = region,
            language = language,
            kbEntry = kbEntry
        )

        // Clean through GuardrailFilter
        val safeText = GuardrailFilter.sanitize(adviceText)

        // Stream tokens word-by-word with ~30ms delay
        val words = safeText.split(" ")
        for (i in words.indices) {
            val token = if (i == words.size - 1) words[i] else words[i] + " "
            onToken(token)
            delay(30)
        }
    }

    override fun close() {
        // No-op
    }
}
