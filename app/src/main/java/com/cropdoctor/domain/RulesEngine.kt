package com.cropdoctor.domain

import com.cropdoctor.R
import com.cropdoctor.knowledge.KbEntry

object RulesEngine {

    const val EXTENSION_OFFICER_NUMBER = "18001801551"
    const val EXTENSION_OFFICER_LABEL = "Kisan Call Centre"

    enum class OpenTier {
        NATURAL_FIRST,
        BOTH_SHOWN,
        CHEMICAL_FIRST
    }

    /**
     * Maps raw classifier confidence to farmer-readable certainty bands:
     * - Sure: >= 0.80
     * - Probably: 0.60 to 0.799
     * - Not sure: < 0.60
     */
    fun calculateCertainty(confidence: Float): CertaintyBand {
        return when {
            confidence >= 0.80f -> CertaintyBand.SURE
            confidence >= 0.60f -> CertaintyBand.PROBABLY
            else -> CertaintyBand.NOT_SURE
        }
    }

    /**
     * Calculates severity band from lesion area fraction (< 10% mild, 10-30% moderate, > 30% severe)
     * and specific pathology rules via SeverityCalculator.
     */
    fun calculateSeverity(lesionFraction: Float, classId: String = ""): SeverityBand {
        return SeverityCalculator.calculate(lesionFraction, classId)
    }

    /**
     * openTier decision logic:
     * - Mild: Natural tier opened first
     * - Moderate: Both shown
     * - Severe: Chemical tier + extension worker first
     */
    fun determineOpenTier(severityBand: SeverityBand): OpenTier {
        return when (severityBand) {
            SeverityBand.MILD -> OpenTier.NATURAL_FIRST
            SeverityBand.MODERATE -> OpenTier.BOTH_SHOWN
            SeverityBand.SEVERE -> OpenTier.CHEMICAL_FIRST
        }
    }

    /**
     * Referral rule: Not sure or severe -> red "Talk to an extension officer" button
     */
    fun shouldShowExtensionWorker(certaintyBand: CertaintyBand, severityBand: SeverityBand): Boolean {
        return certaintyBand == CertaintyBand.NOT_SURE || severityBand == SeverityBand.SEVERE
    }

    /**
     * T2b: Determines whether to show severity band to the farmer.
     * When certainty is NOT_SURE, severity must not be shown.
     */
    fun shouldShowSeverity(certaintyBand: CertaintyBand): Boolean {
        return certaintyBand != CertaintyBand.NOT_SURE
    }

    /**
     * T2b: Determines whether to show the early warning action sentence.
     * When certainty is NOT_SURE, early warning must not be shown.
     */
    fun shouldShowEarlyWarning(certaintyBand: CertaintyBand, severityBand: SeverityBand): Boolean {
        return certaintyBand != CertaintyBand.NOT_SURE && severityBand == SeverityBand.MILD
    }

    /**
     * T2b: Resolves guidance / status sentence:
     * - If certainty is NOT_SURE -> R.string.cant_tell_no_disease ("Can’t tell – no disease identified")
     * - If MILD -> R.string.severity_early_warning (or severity_pest_early_warning for pests)
     * - Else -> R.string.severity_act_now (or severity_pest_act_now for pests)
     */
    fun getGuidanceSentence(certaintyBand: CertaintyBand, severityBand: SeverityBand, type: String = "disease"): Int {
        if (certaintyBand == CertaintyBand.NOT_SURE) {
            return R.string.cant_tell_no_disease
        }
        return getSeverityActionSentence(severityBand, type)
    }

    /**
     * Severity action sentence:
     * - Mild -> early warning
     * - Moderate/Severe -> act now
     * Pest results use pest-specific monitoring & ETL guidance (Rule G1).
     */
    fun getSeverityActionSentence(severityBand: SeverityBand, type: String = "disease"): Int {
        if (type.equals("pest", ignoreCase = true)) {
            return when (severityBand) {
                SeverityBand.MILD -> R.string.severity_pest_early_warning
                SeverityBand.MODERATE, SeverityBand.SEVERE -> R.string.severity_pest_act_now
            }
        }
        return when (severityBand) {
            SeverityBand.MILD -> R.string.severity_early_warning
            SeverityBand.MODERATE, SeverityBand.SEVERE -> R.string.severity_act_now
        }
    }

    /**
     * Determines whether the app should prompt the farmer to call an agricultural extension worker (KVK/DAE).
     */
    fun shouldReferToKVK(severityBand: SeverityBand, referralThreshold: String): Boolean {
        return when (referralThreshold.uppercase()) {
            "NONE" -> false
            "MODERATE" -> severityBand == SeverityBand.MODERATE || severityBand == SeverityBand.SEVERE
            "SEVERE" -> severityBand == SeverityBand.SEVERE
            else -> severityBand == SeverityBand.SEVERE
        }
    }

    /**
     * Healthy plants never receive chemical fungicide/pesticide recommendations.
     */
    fun isChemicalTreatmentApplicable(classId: String): Boolean {
        return !classId.endsWith("_healthy")
    }

    /**
     * T2c: Formats the AI Advisory card text according to certainty and severity rules.
     * When certainty is NOT_SURE (or classId is "unknown"):
     * - Must NOT contain any severity line (including "Severity Guidance (MILD)").
     * - Must NOT contain any "Immediate Step" text that depends on severity.
     * - Returns strictly the verified explanation text from the Knowledge Base.
     */
    fun buildAdvisoryText(
        classId: String,
        certaintyBand: CertaintyBand,
        severityBand: SeverityBand,
        language: String,
        kbEntry: KbEntry
    ): String {
        val isNotSure = certaintyBand == CertaintyBand.NOT_SURE || classId.equals("unknown", ignoreCase = true)
        if (isNotSure) {
            return when (language.lowercase()) {
                "te" -> kbEntry.explainText.te
                "hi" -> kbEntry.explainText.hi
                else -> kbEntry.explainText.en
            }
        }

        return when (language.lowercase()) {
            "te" -> {
                val explain = kbEntry.explainText.te
                val immediate = kbEntry.immediateStep.te
                val advice = kbEntry.severityAdvice[severityBand.name]?.te ?: ""
                val immediatePart = if (immediate.isNotBlank()) "\n\nతక్షణ చర్య: $immediate" else ""
                val advicePart = if (advice.isNotBlank()) "\n\nనివారణ సలహా: $advice" else ""
                "$explain$immediatePart$advicePart"
            }
            "hi" -> {
                val explain = kbEntry.explainText.hi
                val immediate = kbEntry.immediateStep.hi
                val advice = kbEntry.severityAdvice[severityBand.name]?.hi ?: ""
                val immediatePart = if (immediate.isNotBlank()) "\n\nतत्काल कदम: $immediate" else ""
                val advicePart = if (advice.isNotBlank()) "\n\nसलाह: $advice" else ""
                "$explain$immediatePart$advicePart"
            }
            else -> {
                val explain = kbEntry.explainText.en
                val immediate = kbEntry.immediateStep.en
                val advice = kbEntry.severityAdvice[severityBand.name]?.en ?: ""
                val immediatePart = if (immediate.isNotBlank()) "\n\nImmediate Step: $immediate" else ""
                val advicePart = if (advice.isNotBlank()) "\n\nSeverity Guidance ($severityBand): $advice" else ""
                "$explain$immediatePart$advicePart"
            }
        }
    }
}
