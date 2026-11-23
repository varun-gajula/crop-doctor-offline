package com.cropdoctor.chat

interface PassageStore {
    /**
     * Stores and indexes passages into SQLite with FTS5.
     */
    fun indexPassages(passages: List<Passage>)

    /**
     * Returns the best 1-3 passages for (detected class, question keywords, language) using SQLite FTS5.
     */
    fun getBestPassages(
        detectedClass: String,
        questionKeywords: String,
        language: String,
        limit: Int = 3
    ): List<Passage>

    /**
     * Retrieves an exact section passage for a class and language.
     */
    fun getPassageBySection(
        detectedClass: String,
        section: String,
        language: String
    ): Passage?

    /**
     * Total number of indexed passages.
     */
    fun countPassages(): Int
}
