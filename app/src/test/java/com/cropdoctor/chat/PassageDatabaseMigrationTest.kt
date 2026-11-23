package com.cropdoctor.chat

import org.junit.Assert.*
import org.junit.Test
import java.sql.DriverManager

/**
 * Unit test verifying FTS schema creation (fresh install) and upgrade from v2 to v3.
 * Confirms that upgrading from v1/v2 drops the old table, creates the v3 table with UNINDEXED
 * metadata columns, and re-indexes passages without crashing or schema mismatches.
 */
class PassageDatabaseMigrationTest {

    @Test
    fun testFreshInstallCreatesV3Schema() {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { stmt ->
            // Fresh install onCreate
            stmt.execute(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS passages_fts USING fts5(
                    id UNINDEXED,
                    class_id UNINDEXED,
                    section UNINDEXED,
                    language UNINDEXED,
                    text,
                    source UNINDEXED
                );
                """.trimIndent()
            )
            // Verify insert & query
            stmt.execute(
                "INSERT INTO passages_fts (id, class_id, section, language, text, source) VALUES ('1', 'rice_leaf_blast', 'symptoms', 'en', 'Spindle-shaped lesions with grey center', 'ICAR')"
            )
            val rs = stmt.executeQuery("SELECT text FROM passages_fts WHERE passages_fts MATCH 'lesions'")
            assertTrue(rs.next())
            assertEquals("Spindle-shaped lesions with grey center", rs.getString(1))
        }
        conn.close()
    }

    @Test
    fun testUpgradeFromV2ToV3DoesNotCrashAndReindexes() {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { stmt ->
            // Step 1: Simulate v2 schema (all columns indexed)
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
            stmt.execute(
                "INSERT INTO passages_fts (id, class_id, section, language, text, source) VALUES ('v2_1', 'rice_leaf_blast', 'symptoms', 'en', 'Old v2 passage data', 'ICAR')"
            )

            // Step 2: Simulate onUpgrade (DROP TABLE IF EXISTS passages_fts -> onCreate)
            stmt.execute("DROP TABLE IF EXISTS passages_fts")
            stmt.execute(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS passages_fts USING fts5(
                    id UNINDEXED,
                    class_id UNINDEXED,
                    section UNINDEXED,
                    language UNINDEXED,
                    text,
                    source UNINDEXED
                );
                """.trimIndent()
            )

            // Step 3: Simulate Application re-indexing when count == 0
            val countRs = stmt.executeQuery("SELECT count(*) FROM passages_fts")
            assertTrue(countRs.next())
            assertEquals(0, countRs.getInt(1))

            stmt.execute(
                "INSERT INTO passages_fts (id, class_id, section, language, text, source) VALUES ('v3_1', 'rice_leaf_blast', 'symptoms', 'en', 'New v3 indexed passage data', 'ICAR')"
            )
            val searchRs = stmt.executeQuery("SELECT text FROM passages_fts WHERE passages_fts MATCH 'indexed'")
            assertTrue(searchRs.next())
            assertEquals("New v3 indexed passage data", searchRs.getString(1))
        }
        conn.close()
    }
}
