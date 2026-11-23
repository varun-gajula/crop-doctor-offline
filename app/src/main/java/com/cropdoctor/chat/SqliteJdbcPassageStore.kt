package com.cropdoctor.chat

import java.sql.Connection
import java.sql.DriverManager

/**
 * Standard JDBC SQLite FTS5 passage store for JVM unit tests and offline benchmarking.
 * Implements the identical schema and query semantics as AndroidSqlitePassageDatabase.
 */
class SqliteJdbcPassageStore(
    private val dbUrl: String = "jdbc:sqlite::memory:"
) : PassageStore {

    private val connection: Connection = DriverManager.getConnection(dbUrl).apply {
        createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS passages_fts USING fts5(
                    id UNINDEXED,
                    class_id,
                    section,
                    language,
                    text,
                    source
                );
                """.trimIndent()
            )
        }
    }

    override fun indexPassages(passages: List<Passage>) {
        val sql = "INSERT OR REPLACE INTO passages_fts (id, class_id, section, language, text, source) VALUES (?, ?, ?, ?, ?, ?)"
        connection.autoCommit = false
        try {
            connection.prepareStatement(sql).use { pstmt ->
                for (p in passages) {
                    pstmt.setString(1, p.id)
                    pstmt.setString(2, p.classId)
                    pstmt.setString(3, p.section)
                    pstmt.setString(4, p.language)
                    pstmt.setString(5, p.text)
                    pstmt.setString(6, p.source)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }
            connection.commit()
        } finally {
            connection.autoCommit = true
        }
    }

    override fun getPassageBySection(
        detectedClass: String,
        section: String,
        language: String
    ): Passage? {
        val sql = "SELECT id, class_id, section, language, text, source FROM passages_fts WHERE class_id = ? AND language = ? AND section = ? LIMIT 1"
        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, detectedClass)
            pstmt.setString(2, language)
            pstmt.setString(3, section)
            val rs = pstmt.executeQuery()
            return if (rs.next()) {
                Passage(
                    id = rs.getString(1),
                    classId = rs.getString(2),
                    section = rs.getString(3),
                    language = rs.getString(4),
                    text = rs.getString(5),
                    source = rs.getString(6)
                )
            } else {
                null
            }
        }
    }

    override fun getBestPassages(
        detectedClass: String,
        questionKeywords: String,
        language: String,
        limit: Int
    ): List<Passage> {
        val results = mutableListOf<Passage>()

        // 1. Direct section keyword mapping
        val matchedSection = QuestionKeywordMatcher.matchSection(questionKeywords, language)
        if (matchedSection != null) {
            val direct = getPassageBySection(detectedClass, matchedSection, language)
            if (direct != null) {
                results.add(direct)
            }
        }

        // 2. FTS5 ranked search
        val stopWords = setOf(
            "what", "is", "it", "the", "in", "of", "to", "a", "an", "on", "at", "for", "and", "or",
            "by", "with", "from", "today", "per", "how", "why", "when", "where", "who", "can", "i",
            "you", "he", "she", "they", "we", "my", "your", "this", "that", "there", "here",
            "क्या", "है", "यह", "वह", "में", "का", "की", "के", "को", "पर", "से", "और", "या",
            "ఏమిటి", "ఇది", "అది", "లో", "యొక్క", "మరియు", "లేదా", "నుండి", "తో"
        )
        val cleanTokens = questionKeywords.split(Regex("\\s+"))
            .map { it.filter { ch -> ch.isLetterOrDigit() }.lowercase() }
            .filter { it.length >= 3 && it !in stopWords }

        if (cleanTokens.isNotEmpty()) {
            val ftsQuery = cleanTokens.joinToString(" OR ") { if (it.length >= 4) "$it*" else it }
            val sql = "SELECT id, class_id, section, language, text, source FROM passages_fts WHERE class_id = ? AND language = ? AND passages_fts MATCH ? ORDER BY rank LIMIT ?"
            try {
                connection.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, detectedClass)
                    pstmt.setString(2, language)
                    pstmt.setString(3, ftsQuery)
                    pstmt.setInt(4, limit)
                    val rs = pstmt.executeQuery()
                    while (rs.next()) {
                        val p = Passage(
                            id = rs.getString(1),
                            classId = rs.getString(2),
                            section = rs.getString(3),
                            language = rs.getString(4),
                            text = rs.getString(5),
                            source = rs.getString(6)
                        )
                        if (results.none { it.id == p.id }) {
                            results.add(p)
                        }
                        if (results.size >= limit) break
                    }
                }
            } catch (e: Exception) {
                // Ignore FTS syntax error
            }
        }

        return results.take(limit)
    }

    override fun countPassages(): Int {
        val sql = "SELECT COUNT(*) FROM passages_fts"
        connection.createStatement().use { stmt ->
            val rs = stmt.executeQuery(sql)
            return if (rs.next()) rs.getInt(1) else 0
        }
    }

    fun close() {
        if (!connection.isClosed) {
            connection.close()
        }
    }
}
