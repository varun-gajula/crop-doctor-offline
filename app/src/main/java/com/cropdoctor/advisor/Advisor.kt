package com.cropdoctor.advisor

import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry

interface Advisor {
    val isFake: Boolean get() = false
    val isLowMemoryFallback: Boolean get() = false
    val isFallbackToOfflineGuide: Boolean get() = false

    suspend fun generateAdvice(
        classId: String,
        severityBand: SeverityBand,
        certainty: String,
        language: String, // "en", "te", "hi"
        kbEntry: KbEntry,
        fieldSpread: String = "some",
        region: String = "telangana",
        urgency: RulesEngine.UrgencyLevel? = null,
        onToken: (String) -> Unit
    )

    fun stop() {}

    fun close()
}
