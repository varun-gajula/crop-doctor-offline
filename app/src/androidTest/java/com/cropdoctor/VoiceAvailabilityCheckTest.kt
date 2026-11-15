package com.cropdoctor

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class VoiceAvailabilityCheckTest {

    private val TAG = "VoiceCheck"

    @Test
    fun testVoiceAvailabilityOnDevice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val latch = CountDownLatch(1)
        var ttsEngine: TextToSpeech? = null
        var initStatus = -1

        ttsEngine = TextToSpeech(context) { status ->
            initStatus = status
            latch.countDown()
        }

        val completed = latch.await(10, TimeUnit.SECONDS)
        assertNotNull("TTS engine initialization timed out", completed)

        val teLocale = Locale("te", "IN")
        val hiLocale = Locale("hi", "IN")
        val enLocale = Locale("en", "US")

        val teAvail = ttsEngine?.isLanguageAvailable(teLocale) ?: -999
        val hiAvail = ttsEngine?.isLanguageAvailable(hiLocale) ?: -999
        val enAvail = ttsEngine?.isLanguageAvailable(enLocale) ?: -999

        fun codeToString(code: Int): String = when (code) {
            TextToSpeech.LANG_AVAILABLE -> "LANG_AVAILABLE (0)"
            TextToSpeech.LANG_COUNTRY_AVAILABLE -> "LANG_COUNTRY_AVAILABLE (1)"
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "LANG_COUNTRY_VAR_AVAILABLE (2)"
            TextToSpeech.LANG_MISSING_DATA -> "LANG_MISSING_DATA (-1)"
            TextToSpeech.LANG_NOT_SUPPORTED -> "LANG_NOT_SUPPORTED (-2)"
            else -> "UNKNOWN ($code)"
        }

        val resultLog = buildString {
            appendLine("=== TTS VOICE AVAILABILITY CHECK ON DEVICE ===")
            appendLine("TTS Init Status: $initStatus")
            appendLine("English (en-US): ${codeToString(enAvail)}")
            appendLine("Telugu (te-IN): ${codeToString(teAvail)}")
            appendLine("Hindi (hi-IN): ${codeToString(hiAvail)}")
            appendLine("==============================================")
        }

        Log.i(TAG, resultLog)
        println(resultLog)

        ttsEngine?.shutdown()
    }
}
