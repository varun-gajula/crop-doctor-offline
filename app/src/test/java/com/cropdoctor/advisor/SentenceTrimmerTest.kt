package com.cropdoctor.advisor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceTrimmerTest {

    @Test
    fun testCompleteEnglishSentenceReturnsUnchanged() {
        val text = "1. What I see: Leaf curls upwards. 2. What to do now: Spray neem oil. 3. When to get help: Call 1800-180-1551."
        val (clean, wasCut) = SentenceTrimmer.cleanToCompleteSentence(text, "en")
        assertEquals(text, clean)
        assertFalse("Complete sentence should not be cut", wasCut)
    }

    @Test
    fun testCompleteHindiSentenceWithDandaReturnsUnchanged() {
        val text = "1. मैंने देखा: पत्तियां मुड़ रही हैं। 2. अब क्या करें: नीम के तेल का छिड़काव करें। 3. सहायता कब लें: कॉल 1800-180-1551।"
        val (clean, wasCut) = SentenceTrimmer.cleanToCompleteSentence(text, "hi")
        assertEquals(text, clean)
        assertFalse("Complete Hindi sentence should not be cut", wasCut)
    }

    @Test
    fun testMidSentenceCutoffTrimsToLastSentence() {
        val text = "1. What I see: Leaf curls upwards. 2. What to do now: Spray neem oil. 3. When to get help: Call extension officer if the condition worsens or if"
        val (clean, wasCut) = SentenceTrimmer.cleanToCompleteSentence(text, "en")
        assertEquals(
            "1. What I see: Leaf curls upwards. 2. What to do now: Spray neem oil.",
            clean
        )
        assertTrue("Incomplete tail should be cut", wasCut)
    }

    @Test
    fun testHindiMidSentenceCutoffTrimsToLastDanda() {
        val text = "1. मैंने देखा: पत्तियां मुड़ रही हैं। 2. अब क्या करें: नीम के तेल का छिड़काव करें। 3. सहायता कब लें: यदि रोग अधिक फैले तो"
        val (clean, wasCut) = SentenceTrimmer.cleanToCompleteSentence(text, "hi")
        assertEquals(
            "1. मैंने देखा: पत्तियां मुड़ रही हैं। 2. अब क्या करें: नीम के तेल का छिड़काव करें।",
            clean
        )
        assertTrue("Incomplete Hindi tail should be cut", wasCut)
    }

    @Test
    fun testNoSentenceTerminatorReturnsEmptyAndWasCut() {
        val text = "Leaf curls upwards without any punctuation"
        val (clean, wasCut) = SentenceTrimmer.cleanToCompleteSentence(text, "en")
        assertEquals("", clean)
        assertTrue("No terminator should mark wasCut true", wasCut)
    }

    @Test
    fun testSpecialTokensAreStrippedWithoutDuplicatingOrCorruptingSentence() {
        val text = "1. What I see: Healthy leaf. 2. What to do now: Maintain optimal water.<|eot_id|>"
        val (clean, wasCut) = SentenceTrimmer.cleanToCompleteSentence(text, "en")
        assertEquals(
            "1. What I see: Healthy leaf. 2. What to do now: Maintain optimal water.",
            clean
        )
        assertFalse("Trailing special token should be stripped and clean sentence retained", wasCut)
    }
}
