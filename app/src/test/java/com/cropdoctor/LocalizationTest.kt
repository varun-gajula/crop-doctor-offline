package com.cropdoctor

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationTest {

    private fun loadStringKeysAndValues(file: File): Map<String, String> {
        assertTrue("String resource file must exist: ${file.absolutePath}", file.exists())
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            val value = node.textContent
            if (name != null) {
                map[name] = value
            }
        }
        return map
    }

    private val baseResDir = File("src/main/res")

    @Test
    fun testAllKeysPresentInAllLanguages() {
        val enFile = File(baseResDir, "values/strings.xml")
        val teFile = File(baseResDir, "values-te/strings.xml")
        val hiFile = File(baseResDir, "values-hi/strings.xml")

        val enMap = loadStringKeysAndValues(enFile)
        val teMap = loadStringKeysAndValues(teFile)
        val hiMap = loadStringKeysAndValues(hiFile)

        assertFalse("English strings must not be empty", enMap.isEmpty())

        val missingInTe = enMap.keys - teMap.keys
        val extraInTe = teMap.keys - enMap.keys
        assertTrue("Telugu strings missing keys: $missingInTe", missingInTe.isEmpty())
        assertTrue("Telugu strings has extra keys: $extraInTe", extraInTe.isEmpty())

        val missingInHi = enMap.keys - hiMap.keys
        val extraInHi = hiMap.keys - enMap.keys
        assertTrue("Hindi strings missing keys: $missingInHi", missingInHi.isEmpty())
        assertTrue("Hindi strings has extra keys: $extraInHi", extraInHi.isEmpty())
    }

    @Test
    fun testEnglishHasNoIndicCharacters() {
        val enFile = File(baseResDir, "values/strings.xml")
        val enMap = loadStringKeysAndValues(enFile)

        // Telugu range: \u0C00-\u0C7F, Devanagari range: \u0900-\u097F
        val indicRegex = Regex("[\\u0C00-\\u0C7F\\u0900-\\u097F]")

        for ((key, value) in enMap) {
            assertFalse(
                "English key '$key' contains Indic characters: '$value'",
                indicRegex.containsMatchIn(value)
            )
        }
    }

    @Test
    fun testTeluguStringsHaveTeluguCharacters() {
        val teFile = File(baseResDir, "values-te/strings.xml")
        val teMap = loadStringKeysAndValues(teFile)
        val teluguRegex = Regex("[\\u0C00-\\u0C7F]")

        // Keys that are numbers or symbols or brand words might not need Telugu, but prose keys must
        val proseKeys = teMap.keys.filterNot { it in listOf("close", "offline_badge") }
        for (key in proseKeys) {
            val value = teMap[key] ?: ""
            assertTrue(
                "Telugu key '$key' should be localized in Telugu, was: '$value'",
                teluguRegex.containsMatchIn(value)
            )
        }
    }

    @Test
    fun testHindiStringsHaveDevanagariCharacters() {
        val hiFile = File(baseResDir, "values-hi/strings.xml")
        val hiMap = loadStringKeysAndValues(hiFile)
        val devanagariRegex = Regex("[\\u0900-\\u097F]")

        val proseKeys = hiMap.keys.filterNot { it in listOf("close", "offline_badge") }
        for (key in proseKeys) {
            val value = hiMap[key] ?: ""
            assertTrue(
                "Hindi key '$key' should be localized in Devanagari, was: '$value'",
                devanagariRegex.containsMatchIn(value)
            )
        }
    }
}
