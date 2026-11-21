package com.cropdoctor.advisor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry

/**
 * FakeAdvisor exists ONLY in unit test/debug code (Task A-REAL).
 * Used for automated unit tests without native ExecuTorch dependencies.
 * Contains ZERO artificial delay streaming (delay(30) removed).
 */
class FakeAdvisor : Advisor {

    override val isFake: Boolean = true
    override val advisorSource: String = "OFFLINE_GUIDE"
    override val isModelAvailable: Boolean = false

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

        // Clean through GuardrailFilter and emit immediately without fake streaming
        val safeText = GuardrailFilter.sanitize(adviceText)
        onToken(safeText)
    }

    override fun close() {
        // No-op
    }
}
