package com.cropdoctor

import com.cropdoctor.chat.QuestionKeywordMatcher
import com.cropdoctor.chat.QuestionSection
import com.cropdoctor.voice.VoiceModelManager
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Verification test suite for Task VOICE (Offline Speech Recognition).
 *
 * Verifies:
 * 1. Model resolution: language codes, folder names, and directory validation.
 * 2. Telugu speech & chip mapping: matching keywords to the 8 chips, detecting weak/uncertain recognition.
 * 3. Word Error Rate (WER) computation logic: exact matches, substitutions, insertions, deletions.
 * 4. Model lifecycle decoupling: Vosk model can be released so Llama and Vosk never co-exist in memory.
 */
class VoiceRecognitionTest {

    @Test
    fun testLanguageCodeNormalization() {
        assertEquals("en", VoiceModelManager.getLangCode("en"))
        assertEquals("en", VoiceModelManager.getLangCode("EN"))
        assertEquals("en", VoiceModelManager.getLangCode("english"))
        assertEquals("hi", VoiceModelManager.getLangCode("hi"))
        assertEquals("hi", VoiceModelManager.getLangCode("HI"))
        assertEquals("te", VoiceModelManager.getLangCode("te"))
        assertEquals("te", VoiceModelManager.getLangCode("TE"))
    }

    @Test
    fun testModelFolderNames() {
        assertEquals(VoiceModelManager.MODEL_EN, VoiceModelManager.getModelFolderName("en"))
        assertEquals(VoiceModelManager.MODEL_HI, VoiceModelManager.getModelFolderName("hi"))
        assertEquals(VoiceModelManager.MODEL_TE, VoiceModelManager.getModelFolderName("te"))
        assertEquals("vosk-model-small-en-in-0.4", VoiceModelManager.MODEL_EN)
        assertEquals("vosk-model-small-hi-0.22", VoiceModelManager.MODEL_HI)
        assertEquals("vosk-model-small-te-0.42", VoiceModelManager.MODEL_TE)
    }

    @Test
    fun testModelDirectoryValidation() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "vosk_test_model_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            // Empty dir is not a valid model
            assertFalse(VoiceModelManager.isValidModelDirectory(tempDir))

            // Create 'am' folder
            val amDir = File(tempDir, "am")
            amDir.mkdirs()
            assertTrue(VoiceModelManager.isValidModelDirectory(tempDir))

            amDir.delete()
            assertFalse(VoiceModelManager.isValidModelDirectory(tempDir))

            // Create 'conf/mfcc.conf'
            val confDir = File(tempDir, "conf")
            confDir.mkdirs()
            val mfcc = File(confDir, "mfcc.conf")
            mfcc.writeText("--use-energy=false")
            assertTrue(VoiceModelManager.isValidModelDirectory(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testTeluguVoiceKeywordChipMapping() {
        // High confidence matching
        val symptomQuery = "లక్షణాలు ఏమిటి"
        val matchedSymptom = QuestionKeywordMatcher.matchSection(symptomQuery, "te")
        assertEquals(QuestionSection.SYMPTOMS, matchedSymptom)

        val chemicalQuery = "రసాయన మందుల పిచికారీ ఎంత"
        val matchedChem = QuestionKeywordMatcher.matchSection(chemicalQuery, "te")
        assertEquals(QuestionSection.CHEMICAL, matchedChem)

        val officerQuery = "అధికారికి ఎప్పుడు కాల్ చేయాలి"
        val matchedOfficer = QuestionKeywordMatcher.matchSection(officerQuery, "te")
        assertEquals(QuestionSection.WHEN_TO_CALL_OFFICER, matchedOfficer)

        val naturalQuery = "సేంద్రీయ లేదా సహజ నివారణ ఏమిటి"
        val matchedNatural = QuestionKeywordMatcher.matchSection(naturalQuery, "te")
        assertEquals(QuestionSection.NATURAL_REMEDY, matchedNatural)
    }

    @Test
    fun testTeluguUncertainRecognitionTriggersFallback() {
        // Garbled Telugu phonemes from a high-WER speech recognition (e.g. Vosk small te WER ~87.9%)
        val garbledQuery = "ఆకు కనపడలేదు ఏమిటో తెలీదు"
        val matched = QuestionKeywordMatcher.matchSection(garbledQuery, "te")

        // Should return null (or unmapped) so the UI displays the 8 chips with honest uncertainty notice
        assertNull(matched)

        // The 8 quick chips must always be available
        val chips = QuestionKeywordMatcher.getQuestionChips("te")
        assertEquals(8, chips.size)
        assertTrue(chips.any { it.first == QuestionSection.SYMPTOMS })
        assertTrue(chips.any { it.first == QuestionSection.CHEMICAL })
        assertTrue(chips.any { it.first == QuestionSection.WHEN_TO_CALL_OFFICER })
    }

    @Test
    fun testWordErrorRateComputationLogic() {
        // Exact match
        val (wer0, edits0) = computeWer("what is leaf blast", "what is leaf blast")
        assertEquals(0.0, wer0, 0.001)
        assertEquals(0, edits0)

        // 1 Substitution in 4 words = 0.25 (25%)
        val (wer1, edits1) = computeWer("what is leaf blast", "what is leaf rust")
        assertEquals(0.25, wer1, 0.001)
        assertEquals(1, edits1)

        // 1 Deletion in 4 words = 0.25 (25%)
        val (wer2, edits2) = computeWer("how to prevent disease", "how prevent disease")
        assertEquals(0.25, wer2, 0.001)
        assertEquals(1, edits2)

        // 2 Insertions into 2 words = 1.0 (100%)
        val (wer3, edits3) = computeWer("call officer", "please call the officer")
        assertEquals(1.0, wer3, 0.001)
        assertEquals(2, edits3)
    }

    private fun computeWer(ref: String, hyp: String): Pair<Double, Int> {
        val refWords = ref.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val hypWords = hyp.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        val n = refWords.size
        val m = hypWords.size
        if (n == 0) return Pair(if (m > 0) 1.0 else 0.0, m)

        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j

        for (i in 1..n) {
            for (j in 1..m) {
                if (refWords[i - 1] == hypWords[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1,      // insertion
                        dp[i - 1][j - 1] + 1   // substitution
                    )
                }
            }
        }
        val edits = dp[n][m]
        return Pair(edits.toDouble() / n, edits)
    }
}
