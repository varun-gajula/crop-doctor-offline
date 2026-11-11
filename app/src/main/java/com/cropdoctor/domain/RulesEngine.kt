package com.cropdoctor.domain

object RulesEngine {

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

