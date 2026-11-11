package com.cropdoctor.advisor

import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import kotlinx.coroutines.delay

/**
 * FakeAdvisor enables UI testing, demonstration, and emulator development.
 * It simulates streaming token generation from Llama 3.2 1B with grounded, safe advice.
 */
class FakeAdvisor : Advisor {

    override suspend fun generateAdvice(
        classId: String,
        severityBand: SeverityBand,
        certainty: String,
        language: String,
        kbEntry: KbEntry,
        onToken: (String) -> Unit
    ) {
        // Build grounded advice based on language
        val adviceText = when (language.lowercase()) {
            "te" -> {
                // Telugu is always deterministic and strictly grounded from verified ICAR KB
                val explain = kbEntry.explainText.te
                val immediate = kbEntry.immediateStep.te
                val advice = kbEntry.severityAdvice[severityBand.name]?.te ?: ""
                "$explain\n\nతక్షణ చర్య: $immediate\n\nనివారణ సలహా: $advice"
            }
            "hi" -> {
                val explain = kbEntry.explainText.hi
                val immediate = kbEntry.immediateStep.hi
                val advice = kbEntry.severityAdvice[severityBand.name]?.hi ?: ""
                "$explain\n\nतत्काल कदम: $immediate\n\nसलाह: $advice"
            }
            else -> {
                val explain = kbEntry.explainText.en
                val immediate = kbEntry.immediateStep.en
                val advice = kbEntry.severityAdvice[severityBand.name]?.en ?: ""
                "$explain\n\nImmediate Step: $immediate\n\nSeverity Guidance ($severityBand): $advice"
            }
        }

        // Clean through GuardrailFilter
        val safeText = GuardrailFilter.sanitize(adviceText)

        // Stream tokens word-by-word with ~40ms delay (simulating ~15-20 tokens/sec on Arm CPU)
        val words = safeText.split(" ")
        for (i in words.indices) {
            val token = if (i == words.size - 1) words[i] else words[i] + " "
            onToken(token)
            delay(40)
        }
    }

    override fun close() {
        // No-op
    }
}
