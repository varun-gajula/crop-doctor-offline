package com.cropdoctor.advisor

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class OfflineTtsHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "OfflineTtsHelper"
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            Log.i(TAG, "Offline TextToSpeech engine initialized successfully.")
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String, language: String) {
        if (!isReady || tts == null) {
            Log.w(TAG, "TTS not ready yet")
            return
        }

        // Clean markdown symbols (*, #, _, •) for smooth natural speech
        val cleanText = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("•", "")
            .replace("⚠️", "")
            .replace("🌱", "")
            .replace("🧪", "")
            .replace("💧", "")
            .replace("🛡️", "")
            .replace("🌾", "")
            .replace("💡", "")
            .trim()

        val locale = when (language.lowercase()) {
            "te" -> Locale("te", "IN")
            "hi" -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }

        try {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default or English if specific dialect voice isn't present
                tts?.language = Locale.ENGLISH
            }
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "CropDoctorTTS_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "TTS speech error", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

