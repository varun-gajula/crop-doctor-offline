package com.cropdoctor.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream

/**
 * Result data class for speech recognition.
 */
data class VoiceRecognitionResult(
    val text: String,
    val isFinal: Boolean,
    val language: String,
    val durationMs: Long = 0L,
    val matchedSection: String? = null,
    val isUncertain: Boolean = false
)

/**
 * Lifecycle-aware offline voice recognizer using Vosk (Task VOICE).
 *
 * Enforces Rule H15 & Requirement 3:
 * Speech model is loaded ONLY while recording or recognizing audio,
 * and is immediately released when finished so Llama and Vosk never share memory.
 */
class VoskVoiceRecognizer(private val context: Context) {

    companion object {
        private const val TAG = "VoskVoiceRecognizer"
        private const val SAMPLE_RATE = 16000
    }

    private var activeModel: Model? = null
    private var activeRecognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isRecording: Boolean = false
        private set

    /**
     * Checks whether an offline model for [language] is installed on device.
     */
    fun isModelAvailable(language: String): Boolean {
        return VoiceModelManager.isModelInstalled(context, language)
    }

    /**
     * Starts audio recording and real-time offline recognition for [language].
     *
     * @param language "en", "hi", or "te"
     * @param onPartial emitted live as the user speaks
     * @param onFinal emitted when recording ends with the final recognized text
     * @param onError emitted on audio hardware error or missing model
     */
    fun startListening(
        language: String,
        onPartial: (String) -> Unit = {},
        onFinal: (VoiceRecognitionResult) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (isRecording) {
            stopListening()
        }

        val modelDir = VoiceModelManager.findModelDir(context, language)
        if (modelDir == null) {
            val langName = when (language.lowercase()) {
                "te" -> "తెలుగు (Telugu)"
                "hi" -> "हिन्दी (Hindi)"
                else -> "English"
            }
            onError("Speech model for $langName is not installed. Please install it in Settings.")
            return
        }

        val startTime = System.currentTimeMillis()

        recordingJob = scope.launch {
            try {
                Log.i(TAG, "Loading Vosk speech model for '$language' from: ${modelDir.absolutePath}")
                val model = Model(modelDir.absolutePath)
                activeModel = model
                val recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
                activeRecognizer = recognizer

                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                val record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    withContext(Dispatchers.Main) {
                        onError("Microphone initialization failed. Please check permissions.")
                    }
                    releaseModel()
                    return@launch
                }

                audioRecord = record
                record.startRecording()
                isRecording = true
                Log.i(TAG, "Audio recording started at 16kHz mono.")

                val buffer = ShortArray(bufferSize / 2)
                val fullTextBuilder = StringBuilder()

                while (isActive && isRecording) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        if (recognizer.acceptWaveForm(buffer, read)) {
                            val resJson = recognizer.result
                            val text = parseResultText(resJson)
                            if (text.isNotBlank()) {
                                if (fullTextBuilder.isNotEmpty()) fullTextBuilder.append(" ")
                                fullTextBuilder.append(text)
                                withContext(Dispatchers.Main) {
                                    onPartial(fullTextBuilder.toString().trim())
                                }
                            }
                        } else {
                            val partialJson = recognizer.partialResult
                            val partial = parsePartialText(partialJson)
                            if (partial.isNotBlank()) {
                                val current = if (fullTextBuilder.isNotEmpty()) {
                                    "${fullTextBuilder} $partial"
                                } else {
                                    partial
                                }
                                withContext(Dispatchers.Main) {
                                    onPartial(current.trim())
                                }
                            }
                        }
                    }
                }

                // Final extraction
                val finalJson = recognizer.finalResult
                val finalChunk = parseResultText(finalJson)
                if (finalChunk.isNotBlank()) {
                    if (fullTextBuilder.isNotEmpty()) fullTextBuilder.append(" ")
                    fullTextBuilder.append(finalChunk)
                }

                val duration = System.currentTimeMillis() - startTime
                val totalText = fullTextBuilder.toString().trim()
                Log.i(TAG, "Speech recognition complete ($duration ms): '$totalText'")

                withContext(Dispatchers.Main) {
                    onFinal(
                        VoiceRecognitionResult(
                            text = totalText,
                            isFinal = true,
                            language = language,
                            durationMs = duration
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Voice recognition error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError("Voice recognition failed: ${e.localizedMessage}")
                }
            } finally {
                releaseModel()
            }
        }
    }

    /**
     * Stops the active audio recording. The final speech recognition result
     * will be processed and delivered via the `onFinal` callback.
     */
    fun stopListening() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore stop errors
        } finally {
            audioRecord = null
        }
    }

    /**
     * Cancels any running recording and immediately frees speech model resources.
     */
    fun cancel() {
        isRecording = false
        recordingJob?.cancel()
        releaseModel()
    }

    /**
     * Releases speech model and recognizer from memory (Rule H15: never in memory with Llama).
     */
    fun releaseModel() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null

        try {
            activeRecognizer?.close()
        } catch (e: Exception) {
            // Ignore
        }
        activeRecognizer = null

        try {
            activeModel?.close()
        } catch (e: Exception) {
            // Ignore
        }
        activeModel = null

        System.gc()
        Log.i(TAG, "Vosk model and audio resources released from RAM.")
    }

    /**
     * Processes an offline WAV file (16kHz 16-bit mono PCM) for testing and benchmarking.
     */
    fun recognizeWavFile(wavFile: File, language: String): VoiceRecognitionResult {
        val modelDir = VoiceModelManager.findModelDir(context, language)
            ?: throw IllegalStateException("Model for $language not found")

        val startTime = System.currentTimeMillis()
        var model: Model? = null
        var recognizer: Recognizer? = null

        try {
            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model, SAMPLE_RATE.toFloat())

            val textBuilder = StringBuilder()
            val buffer = ByteArray(4096)

            FileInputStream(wavFile).use { fis ->
                // Skip 44-byte standard WAV header if present
                val header = ByteArray(44)
                val readHeader = fis.read(header)
                if (readHeader == 44 && String(header, 0, 4) != "RIFF") {
                    // Not standard WAV, rewind by closing and opening
                }

                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        val chunk = parseResultText(recognizer.result)
                        if (chunk.isNotBlank()) {
                            if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                            textBuilder.append(chunk)
                        }
                    }
                }
            }

            val finalChunk = parseResultText(recognizer.finalResult)
            if (finalChunk.isNotBlank()) {
                if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                textBuilder.append(finalChunk)
            }

            val duration = System.currentTimeMillis() - startTime
            return VoiceRecognitionResult(
                text = textBuilder.toString().trim(),
                isFinal = true,
                language = language,
                durationMs = duration
            )
        } finally {
            recognizer?.close()
            model?.close()
            System.gc()
        }
    }

    private fun parseResultText(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            json.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun parsePartialText(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            json.optString("partial", "")
        } catch (e: Exception) {
            ""
        }
    }
}
