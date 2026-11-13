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
        onToken: (String) -> Unit
    ) {
        val certaintyBand = when (certainty.uppercase()) {
            "SURE" -> CertaintyBand.SURE
            "PROBABLY" -> CertaintyBand.PROBABLY
            else -> CertaintyBand.NOT_SURE
        }

        // T2c: Grounded advice based strictly on RulesEngine rules
        val adviceText = RulesEngine.buildAdvisoryText(
            classId = classId,
            certaintyBand = certaintyBand,
            severityBand = severityBand,
            language = language,
            kbEntry = kbEntry
        )

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

