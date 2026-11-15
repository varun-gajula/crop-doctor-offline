package com.cropdoctor.advisor

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.cropdoctor.R
import java.util.Locale

/**
 * Item in the speech / playback queue.
 */
data class AudioPlayItem(
    val pieceId: String? = null,
    val text: String,
    val lang: String,
    val isLlamaOutput: Boolean = false
)

/**
 * Offline Audio Advisor (Requirement F10 / Task A1).
 * Supports:
 * 1. Bundled Opus audio clips from assets for reviewed sentence pieces.
 * 2. System TextToSpeech fallback when clip is missing or for dynamic text.
 * 3. TextToSpeech.isLanguageAvailable check, reporting "Voice not installed" if voice is absent.
 * 4. Rule H6 guardrail: Rejects any attempt to feed Llama-generated text to Telugu audio.
 * 5. Speed control (1.0x normal, 0.75x slow).
 * 6. Play, Pause, Resume, and Stop controls with Compose state observables.
 * 7. Works 100% offline (Airplane mode compatible).
 */
class OfflineAudioAdvisor(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private val TAG = "OfflineAudioAdvisor"

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null
    private var manifest: AudioManifest? = null

    // Playback state
    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _isPaused = mutableStateOf(false)
    val isPaused: State<Boolean> = _isPaused

    private val _playbackSpeed = mutableStateOf(1.0f)
    val playbackSpeed: State<Float> = _playbackSpeed

    private val _statusMessage = mutableStateOf<String?>(null)
    val statusMessage: State<String?> = _statusMessage

    private val _showVoiceInstallDialog = mutableStateOf(false)
    val showVoiceInstallDialog: State<Boolean> = _showVoiceInstallDialog

    private val _missingVoiceLanguage = mutableStateOf("")
    val missingVoiceLanguage: State<String> = _missingVoiceLanguage

    private val queue = mutableListOf<AudioPlayItem>()
    private var currentItemIndex = 0
    private var isCurrentItemMediaPlayer = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
        loadManifest()
    }

    private fun loadManifest() {
        try {
            context.assets.open("audio/manifest.json").use { stream ->
                manifest = AudioManifest.parse(stream)
                Log.i(TAG, "Audio manifest loaded: ${manifest?.clips?.size ?: 0} clips")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load audio/manifest.json, will use TTS fallback: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        _isPlaying.value = true
                        _isPaused.value = false
                    }
                }

                override fun onDone(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        playNextInQueue()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        playNextInQueue()
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Handler(Looper.getMainLooper()).post {
                        playNextInQueue()
                    }
                }
            })
            Log.i(TAG, "Offline TTS initialized successfully.")
        } else {
            Log.e(TAG, "TTS init failed with status: $status")
        }
    }

    /**
     * Plays a single piece of text.
     */
    fun playText(
        text: String,
        language: String,
        isLlamaOutput: Boolean = false,
        pieceId: String? = null
    ) {
        if (text.isBlank()) return
        val items = listOf(
            AudioPlayItem(
                pieceId = pieceId,
                text = text,
                lang = language,
                isLlamaOutput = isLlamaOutput
            )
        )
        playSequence(items)
    }

    /**
     * Plays a sequence of audio items in order.
     */
    fun playSequence(items: List<AudioPlayItem>) {
        stop()
        if (items.isEmpty()) return

        // Rule H6: Telugu is NEVER generated by Llama
        for (item in items) {
            if (item.lang.lowercase() == "te" && item.isLlamaOutput) {
                throw IllegalArgumentException("Rule H6: Telugu text must never be generated by or sent to Llama.")
            }
        }

        queue.clear()
        queue.addAll(items)
        currentItemIndex = 0
        _isPlaying.value = true
        _isPaused.value = false

        playCurrentItem()
    }

    private fun playCurrentItem() {
        if (currentItemIndex >= queue.size) {
            finishPlayback()
            return
        }

        val item = queue[currentItemIndex]
        val clip = item.pieceId?.let { manifest?.clips?.get(it) }

        if (clip != null && AudioManifest.verifyClip(clip)) {
            // Attempt playing the Opus clip from assets
            val played = playOpusClip(clip.file)
            if (played) {
                isCurrentItemMediaPlayer = true
                return
            }
        }

        // Fallback to TTS for this piece
        isCurrentItemMediaPlayer = false
        playTts(item.text, item.lang)
    }

    private fun playOpusClip(assetRelativePath: String): Boolean {
        return try {
            val afd: AssetFileDescriptor = context.assets.openFd(assetRelativePath)
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(_playbackSpeed.value)
                }
                setOnCompletionListener {
                    playNextInQueue()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error ($what, $extra), falling back to TTS")
                    releaseMediaPlayer()
                    val item = queue.getOrNull(currentItemIndex)
                    if (item != null) {
                        playTts(item.text, item.lang)
                    } else {
                        playNextInQueue()
                    }
                    true
                }
                start()
            }
            _isPlaying.value = true
            _isPaused.value = false
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play opus clip $assetRelativePath: ${e.message}")
            releaseMediaPlayer()
            false
        }
    }

    private fun playTts(text: String, language: String) {
        if (!isTtsReady || tts == null) {
            Log.w(TAG, "TTS not ready yet")
            playNextInQueue()
            return
        }

        val cleanText = cleanForSpeech(text)
        val locale = when (language.lowercase()) {
            "te" -> Locale("te", "IN")
            "hi" -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }

        val avail = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (avail == TextToSpeech.LANG_MISSING_DATA || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
            val msg = context.getString(R.string.voice_not_installed)
            _statusMessage.value = msg
            _missingVoiceLanguage.value = language
            _showVoiceInstallDialog.value = true
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            Log.w(TAG, "TTS voice not available for $language ($avail)")
            playNextInQueue()
            return
        }

        try {
            tts?.language = locale
            tts?.setSpeechRate(_playbackSpeed.value)
            val utteranceId = "CropDoctor_${System.currentTimeMillis()}_$currentItemIndex"
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            _isPlaying.value = true
            _isPaused.value = false
        } catch (e: Exception) {
            Log.e(TAG, "TTS speech error", e)
            playNextInQueue()
        }
    }

    private fun playNextInQueue() {
        currentItemIndex++
        if (currentItemIndex < queue.size) {
            playCurrentItem()
        } else {
            finishPlayback()
        }
    }

    private fun finishPlayback() {
        _isPlaying.value = false
        _isPaused.value = false
        currentItemIndex = 0
        queue.clear()
        releaseMediaPlayer()
    }

    /**
     * Pauses current playback (MediaPlayer or TTS).
     */
    fun pause() {
        if (!_isPlaying.value) return

        if (isCurrentItemMediaPlayer && mediaPlayer?.isPlaying == true) {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                Log.w(TAG, "Pause error: ${e.message}")
            }
        } else {
            tts?.stop()
        }

        _isPlaying.value = false
        _isPaused.value = true
    }

    /**
     * Resumes playback if paused.
     */
    fun resume() {
        if (!_isPaused.value) return

        if (isCurrentItemMediaPlayer && mediaPlayer != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(_playbackSpeed.value) ?: return
                }
                mediaPlayer?.start()
                _isPlaying.value = true
                _isPaused.value = false
            } catch (e: Exception) {
                Log.w(TAG, "Resume error: ${e.message}")
                playCurrentItem()
            }
        } else {
            // Resume TTS by re-playing the current item
            _isPlaying.value = true
            _isPaused.value = false
            playCurrentItem()
        }
    }

    /**
     * Stops all playback and resets the queue.
     */
    fun stop() {
        try {
            releaseMediaPlayer()
            tts?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        _isPlaying.value = false
        _isPaused.value = false
        currentItemIndex = 0
        queue.clear()
    }

    /**
     * Checks if TTS voice data is installed and available for the given language code ("te", "hi", "en").
     * Returns TextToSpeech availability status.
     * If missing, sets showVoiceInstallDialog to true.
     */
    fun checkLanguageAvailable(language: String): Int {
        val locale = when (language.lowercase()) {
            "te" -> Locale("te", "IN")
            "hi" -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }
        val avail = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (avail == TextToSpeech.LANG_MISSING_DATA || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
            _missingVoiceLanguage.value = language
            _showVoiceInstallDialog.value = true
        }
        return avail
    }

    /**
     * Dismisses the voice install guidance dialog.
     */
    fun dismissVoiceInstallDialog() {
        _showVoiceInstallDialog.value = false
    }

    /**
     * Launches system TTS settings to install voice data once for offline use.
     */
    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open TTS settings", e2)
            }
        }
    }

    /**
     * Changes playback speed between 1.0x (normal) and 0.75x (slow).
     */
    fun setSpeed(speed: Float) {
        val targetSpeed = if (speed <= 0.85f) 0.75f else 1.0f
        _playbackSpeed.value = targetSpeed

        if (isCurrentItemMediaPlayer && mediaPlayer?.isPlaying == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(targetSpeed) ?: return
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating mediaPlayer speed: ${e.message}")
                }
            }
        } else if (_isPlaying.value) {
            tts?.setSpeechRate(targetSpeed)
        }
    }

    fun toggleSpeed() {
        val newSpeed = if (_playbackSpeed.value == 1.0f) 0.75f else 1.0f
        setSpeed(newSpeed)
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
        tts = null
    }

    companion object {
        fun cleanForSpeech(text: String): String {
            return text
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
                .replace("🤖", "")
                .trim()
        }
    }
}
