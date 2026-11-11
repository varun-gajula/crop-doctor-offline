package com.cropdoctor.advisor

import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry

interface Advisor {
    suspend fun generateAdvice(
        classId: String,
        severityBand: SeverityBand,
        certainty: String,
        language: String, // "en", "te", "hi"
        kbEntry: KbEntry,
        onToken: (String) -> Unit
    )
    fun close()
}

