package com.cropdoctor.chat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * On-device Android SQLite FTS5 passage database.
 * Stores every knowledge-base entry as discrete passages for sub-millisecond offline retrieval.
 */
class AndroidSqlitePassageDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION), PassageStore {

    companion object {
        const val DB_NAME = "cropdoctor_passages.db"
        const val DB_VERSION = 2
        private const val TABLE_FTS = "passages_fts"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts5(
                    id UNINDEXED,
                    class_id,
                    section,
                    language,
                    text,
                    source
                );
                """.trimIndent()
            )
        } catch (e: Exception) {
            // Fallback to standard FTS4 if FTS5 module is not available in OEM SQLite build
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts4(
                    id,
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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FTS")
        onCreate(db)
    }

    override fun indexPassages(passages: List<Passage>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT OR REPLACE INTO $TABLE_FTS (id, class_id, section, language, text, source) VALUES (?, ?, ?, ?, ?, ?)"
            )
            for (p in passages) {
                stmt.bindString(1, p.id)
                stmt.bindString(2, p.classId)
                stmt.bindString(3, p.section)
                stmt.bindString(4, p.language)
                stmt.bindString(5, p.text)
                stmt.bindString(6, p.source)
                stmt.executeInsert()
                stmt.clearBindings()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun getPassageBySection(
        detectedClass: String,
        section: String,
        language: String
    ): Passage? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, class_id, section, language, text, source FROM $TABLE_FTS WHERE class_id = ? AND language = ? AND section = ? LIMIT 1",
            arrayOf(detectedClass, language, section)
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                Passage(
                    id = c.getString(0),
                    classId = c.getString(1),
                    section = c.getString(2),
                    language = c.getString(3),
                    text = c.getString(4),
                    source = c.getString(5)
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
        val db = readableDatabase

        // 1. Check if the query matches a recognized section keyword directly
        val matchedSection = QuestionKeywordMatcher.matchSection(questionKeywords, language)
        if (matchedSection != null) {
            val direct = getPassageBySection(detectedClass, matchedSection, language)
            if (direct != null) {
                results.add(direct)
            }
        }

        // 2. Augment with FTS5 BM25 ranked keyword search
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
            try {
                val cursor = try {
                    db.rawQuery(
                        "SELECT id, class_id, section, language, text, source FROM $TABLE_FTS WHERE class_id = ? AND language = ? AND $TABLE_FTS MATCH ? ORDER BY rank LIMIT ?",
                        arrayOf(detectedClass, language, ftsQuery, limit.toString())
                    )
                } catch (_: Exception) {
                    db.rawQuery(
                        "SELECT id, class_id, section, language, text, source FROM $TABLE_FTS WHERE class_id = ? AND language = ? AND $TABLE_FTS MATCH ? LIMIT ?",
                        arrayOf(detectedClass, language, ftsQuery, limit.toString())
                    )
                }
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val p = Passage(
                            id = c.getString(0),
                            classId = c.getString(1),
                            section = c.getString(2),
                            language = c.getString(3),
                            text = c.getString(4),
                            source = c.getString(5)
                        )
                        if (results.none { it.id == p.id }) {
                            results.add(p)
                        }
                        if (results.size >= limit) break
                    }
                }
            } catch (e: Exception) {
                // Ignore FTS syntax errors and keep direct results
            }
        }

        return results.take(limit)
    }

    override fun countPassages(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_FTS", null)
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
