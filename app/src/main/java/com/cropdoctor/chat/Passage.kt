package com.cropdoctor.chat

/**
 * A discrete knowledge passage stored in SQLite FTS5 for offline retrieval.
 *
 * Required columns per Task CHAT specification:
 * (id, class, section, language, text, source)
 */
data class Passage(
    val id: String,
    val classId: String,
    val section: String,
    val language: String,
    val text: String,
    val source: String
)

/**
 * Standard question type sections for agricultural knowledge retrieval.
 */
object QuestionSection {
    const val WHAT_IS_IT = "what_is_it"
    const val SYMPTOMS = "symptoms"
    const val WHAT_TO_DO_NOW = "what_to_do_now"
    const val PREVENTION = "prevention"
    const val NATURAL_REMEDY = "natural_remedy"
    const val CHEMICAL = "chemical"
    const val SAFETY_WAITING_PERIOD = "safety_waiting_period"
    const val WHEN_TO_CALL_OFFICER = "when_to_call_officer"

    val ALL_SECTIONS = listOf(
        WHAT_IS_IT,
        SYMPTOMS,
        WHAT_TO_DO_NOW,
        PREVENTION,
        NATURAL_REMEDY,
        CHEMICAL,
        SAFETY_WAITING_PERIOD,
        WHEN_TO_CALL_OFFICER
    )
}
