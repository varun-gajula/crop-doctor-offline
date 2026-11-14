package com.cropdoctor.advisor

import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry

interface Advisor {
    val isFake: Boolean get() = false
    val isLowMemoryFallback: Boolean get() = false

    suspend fun generateAdvice(
        classId: String,
        severityBand: SeverityBand,
        certainty: String,
        language: String, // "en", "te", "hi"
        kbEntry: KbEntry,
        onToken: (String) -> Unit
    )

    fun stop() {}

    fun close()
}
