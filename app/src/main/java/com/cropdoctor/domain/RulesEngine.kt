package com.cropdoctor.domain

import com.cropdoctor.R

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
     */
    fun calculateSeverity(lesionFraction: Float, classId: String = ""): SeverityBand {
        if (classId.endsWith("_healthy")) {
            return SeverityBand.MILD
        }
        return when {
            lesionFraction < 0.10f -> SeverityBand.MILD
            lesionFraction <= 0.30f -> SeverityBand.MODERATE
            else -> SeverityBand.SEVERE
        }
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
     * - If MILD -> R.string.severity_early_warning
     * - Else -> R.string.severity_act_now
     */
    fun getGuidanceSentence(certaintyBand: CertaintyBand, severityBand: SeverityBand): Int {
        if (certaintyBand == CertaintyBand.NOT_SURE) {
            return R.string.cant_tell_no_disease
        }
        return getSeverityActionSentence(severityBand)
    }

    /**
     * Severity action sentence:
     * - Mild -> early warning
     * - Moderate/Severe -> act now
     */
    fun getSeverityActionSentence(severityBand: SeverityBand): Int {
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
}
