package com.cropdoctor.advisor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.security.MessageDigest

/**
 * Manifest for offline bundled audio clips (Requirement F10 / Task A1).
 * Maps sentence piece IDs to asset opus files, and records SHA-256 hash of the exact text.
 */
@Serializable
data class ClipMetadata(
    val lang: String,
    val file: String,
    val text: String,
    val sha256: String
)

@Serializable
data class AudioManifest(
    val version: String = "1.0.0",
    val total_clips: Int = 0,
    val audio_folder_size_bytes: Long = 0,
    val clips: Map<String, ClipMetadata> = emptyMap()
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun parse(jsonString: String): AudioManifest {
            return json.decodeFromString(serializer(), jsonString)
        }

        fun parse(stream: InputStream): AudioManifest {
            val content = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parse(content)
        }

        fun computeSha256(text: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun verifyClip(clip: ClipMetadata): Boolean {
            return computeSha256(clip.text).equals(clip.sha256, ignoreCase = true)
        }
    }
}
