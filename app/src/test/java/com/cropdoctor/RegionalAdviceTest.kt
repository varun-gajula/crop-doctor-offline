package com.cropdoctor

import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBaseRoot
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Unit tests for Task G2: Region-specific advice (Requirement F6).
 *
 * Requirements verified:
 * 1. Region choices: Telangana, Andhra Pradesh, Other.
 * 2. Each knowledge-base entry has a regional block per region with:
 *    - PJTSAU for Telangana
 *    - ANGRAU for Andhra Pradesh
 *    - ICAR for Other
 *    - differs: boolean
 *    - localized text present only when differs is true (never invented).
 * 3. Helper KbEntry.getRegionalAdvice resolves region keys accurately.
 * 4. Sourced regional differences for chilli_leaf_curl_virus and maize_fall_armyworm.
 */
class RegionalAdviceTest {

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private lateinit var kbRoot: KnowledgeBaseRoot
        private lateinit var kbFile: File

        @BeforeClass
        @JvmStatic
        fun setUp() {
            kbFile = File("src/main/assets/kb/knowledge_base.json")
            if (!kbFile.exists()) {
                kbFile = File("data/kb/knowledge_base.json")
            }
            assertTrue("Knowledge Base JSON must exist", kbFile.exists())
            kbRoot = json.decodeFromString(kbFile.readText())
        }
    }

    @Test
    fun testAll21ClassesHaveRegionalBlocks() {
        assertEquals("Must contain 21 classes", 21, kbRoot.classes.size)
        val requiredRegions = listOf("telangana", "andhra_pradesh", "other")

        for (entry in kbRoot.classes) {
            val regional = entry.regional
            assertNotNull("${entry.classId} must have 'regional' map", regional)
            for (regKey in requiredRegions) {
                assertTrue("${entry.classId} must have regional entry for $regKey", regional.containsKey(regKey))
            }
        }
    }

    @Test
    fun testRegionalSourcesMatchOfficialAuthorities() {
        for (entry in kbRoot.classes) {
            val tg = entry.regional["telangana"]
            assertNotNull("${entry.classId} telangana missing", tg)
            assertEquals("${entry.classId} telangana source must be PJTSAU", "PJTSAU", tg!!.source)

            val ap = entry.regional["andhra_pradesh"]
            assertNotNull("${entry.classId} andhra_pradesh missing", ap)
            assertEquals("${entry.classId} andhra_pradesh source must be ANGRAU", "ANGRAU", ap!!.source)

            val other = entry.regional["other"]
            assertNotNull("${entry.classId} other missing", other)
            assertEquals("${entry.classId} other source must be ICAR", "ICAR", other!!.source)
        }
    }

    @Test
    fun testDiffersFlagIntegrity() {
        for (entry in kbRoot.classes) {
            for ((regKey, regAdvice) in entry.regional) {
                if (regAdvice.differs) {
                    assertNotNull(
                        "${entry.classId}.regional.$regKey has differs=true, so text must not be null",
                        regAdvice.text
                    )
                    val text = regAdvice.text!!
                    assertTrue("${entry.classId}.regional.$regKey must have English text", text.en.isNotBlank())
                    assertTrue("${entry.classId}.regional.$regKey must have Telugu text", text.te.isNotBlank())
                    assertTrue("${entry.classId}.regional.$regKey must have Hindi text", text.hi.isNotBlank())
                } else {
                    val hasText = regAdvice.text != null &&
                            (regAdvice.text!!.en.isNotBlank() || regAdvice.text!!.te.isNotBlank() || regAdvice.text!!.hi.isNotBlank())
                    assertFalse(
                        "${entry.classId}.regional.$regKey has differs=false, so text must be omitted or empty",
                        hasText
                    )
                }
            }
        }
    }

    @Test
    fun testSourcedRegionalDifferences() {
        // Chilli Leaf Curl Virus has specific vector advisories for Telangana (PJTSAU blue sticky traps) and AP (ANGRAU barrier crops)
        val chilli = kbRoot.classes.find { it.classId == "chilli_leaf_curl_virus" }
        assertNotNull(chilli)
        val chilliTg = chilli!!.getRegionalAdvice("Telangana")
        assertTrue(chilliTg.differs)
        assertEquals("PJTSAU", chilliTg.source)
        assertTrue(chilliTg.text!!.en.contains("PJTSAU"))

        val chilliAp = chilli.getRegionalAdvice("Andhra Pradesh")
        assertTrue(chilliAp.differs)
        assertEquals("ANGRAU", chilliAp.source)
        assertTrue(chilliAp.text!!.en.contains("ANGRAU"))

        val chilliOther = chilli.getRegionalAdvice("Other")
        assertFalse(chilliOther.differs)
        assertEquals("ICAR", chilliOther.source)

        // Maize Fall Armyworm has specific trap and intercropping advisories
        val faw = kbRoot.classes.find { it.classId == "maize_fall_armyworm" }
        assertNotNull(faw)
        val fawTg = faw!!.getRegionalAdvice("Telangana")
        assertTrue(fawTg.differs)
        assertEquals("PJTSAU", fawTg.source)

        val fawAp = faw.getRegionalAdvice("Andhra Pradesh")
        assertTrue(fawAp.differs)
        assertEquals("ANGRAU", fawAp.source)

        val fawOther = faw.getRegionalAdvice("Other")
        assertFalse(fawOther.differs)
        assertEquals("ICAR", fawOther.source)
    }

    @Test
    fun testGetRegionalAdviceResolvesCaseInsensitively() {
        val entry = kbRoot.classes.first()

        val r1 = entry.getRegionalAdvice("telangana")
        assertEquals("PJTSAU", r1.source)

        val r2 = entry.getRegionalAdvice("TELANGANA")
        assertEquals("PJTSAU", r2.source)

        val r3 = entry.getRegionalAdvice("Andhra Pradesh")
        assertEquals("ANGRAU", r3.source)

        val r4 = entry.getRegionalAdvice("andhra")
        assertEquals("ANGRAU", r4.source)

        val r5 = entry.getRegionalAdvice("other")
        assertEquals("ICAR", r5.source)

        val r6 = entry.getRegionalAdvice("Unknown Region")
        assertEquals("ICAR", r6.source)
    }
}
