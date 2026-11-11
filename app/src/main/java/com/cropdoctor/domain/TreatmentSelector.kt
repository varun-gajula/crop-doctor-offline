package com.cropdoctor.domain

object TreatmentSelector {

    enum class TreatmentTab {
        NATURAL,
        CHEMICAL
    }

    /**
     * Determines which treatment tier to present first to the farmer:
     * - Mild severity: Natural and traditional remedies first (bio-agents, cultural, neem)
     * - Moderate/Severe: Advanced chemical remedies first (CIB&RC registered actives with safety info)
     */
    fun getInitialTab(severityBand: SeverityBand, classId: String): TreatmentTab {
        if (classId.endsWith("_healthy")) {
            return TreatmentTab.NATURAL
        }
        return when (severityBand) {
            SeverityBand.MILD -> TreatmentTab.NATURAL
            SeverityBand.MODERATE, SeverityBand.SEVERE -> TreatmentTab.CHEMICAL
        }
    }
}
