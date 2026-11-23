package com.cropdoctor.voice

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Manages local offline Vosk speech recognition models on device.
 *
 * Scans directories in priority order:
 * 1. Internal private files (`context.filesDir/vosk/<lang>`)
 * 2. External app files (`context.getExternalFilesDir(null)/vosk/<lang>`)
 * 3. Developer fallback (`/data/local/tmp/vosk/<lang>`)
 * 4. PC/Laptop test directory (`C:/crop-data/vosk_models/<model_name>`)
 */
object VoiceModelManager {

    private const val TAG = "VoiceModelManager"

    const val MODEL_EN = "vosk-model-small-en-in-0.4"
    const val MODEL_EN_US = "vosk-model-small-en-us-0.15"
    const val MODEL_HI = "vosk-model-small-hi-0.22"
    const val MODEL_TE = "vosk-model-small-te-0.42"

    fun getModelFolderName(language: String): String {
        return when (language.lowercase()) {
            "hi" -> MODEL_HI
            "te" -> MODEL_TE
            else -> MODEL_EN
        }
    }

    fun getLangCode(language: String): String {
        return when (language.lowercase()) {
            "hi" -> "hi"
            "te" -> "te"
            else -> "en"
        }
    }

    /**
     * Resolves the directory path containing the uncompressed Vosk model for [language].
     * Returns null if no valid model directory is found.
     */
    fun findModelDir(context: Context, language: String): File? {
        val lang = getLangCode(language)
        val folderName = getModelFolderName(language)

        val candidates = mutableListOf<File>()

        // 1. Internal storage
        candidates.add(File(context.filesDir, "vosk/$lang"))
        candidates.add(File(context.filesDir, "vosk/$folderName"))
        candidates.add(File(context.filesDir, folderName))

        // 2. External storage
        context.getExternalFilesDir(null)?.let { ext ->
            candidates.add(File(ext, "vosk/$lang"))
            candidates.add(File(ext, "vosk/$folderName"))
            candidates.add(File(ext, folderName))
        }

        // 3. Developer temp
        candidates.add(File("/data/local/tmp/vosk/$lang"))
        candidates.add(File("/data/local/tmp/vosk/$folderName"))
        candidates.add(File("/data/local/tmp/$folderName"))

        // 4. Fallback PC path for JVM / emulator
        candidates.add(File("C:/crop-data/vosk_models/$folderName"))
        candidates.add(File("C:/crop-data/vosk_models/$lang"))

        for (dir in candidates) {
            if (isValidModelDirectory(dir)) {
                Log.i(TAG, "Found valid Vosk model for '$lang' at: ${dir.absolutePath}")
                return dir
            }
        }

        Log.w(TAG, "No valid Vosk model found for '$lang' across candidates.")
        return null
    }

    /**
     * A valid Vosk model directory must exist, be a directory, and contain either `am`, `conf`, or `model.conf`.
     */
    fun isValidModelDirectory(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val amDir = File(dir, "am")
        val confDir = File(dir, "conf")
        val mfccConf = File(dir, "conf/mfcc.conf")
        val modelConf = File(dir, "conf/model.conf")
        return amDir.exists() || confDir.exists() || mfccConf.exists() || modelConf.exists() ||
                (dir.listFiles()?.any { it.name.startsWith("am") || it.name.startsWith("conf") } == true)
    }

    /**
     * Checks if a model for [language] is installed and ready.
     */
    fun isModelInstalled(context: Context, language: String): Boolean {
        return findModelDir(context, language) != null
    }

    /**
     * Returns size in megabytes of the installed model directory, or 0 if absent.
     */
    fun getInstalledModelSizeMb(context: Context, language: String): Long {
        val dir = findModelDir(context, language) ?: return 0L
        return getFolderSize(dir) / (1024 * 1024)
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFolderSize(it) }
        return size
    }

    /**
     * Extracts a model zip file provided via Uri into `context.filesDir/vosk/<lang>`.
     */
    fun installModelFromZip(context: Context, zipUri: Uri, language: String): Boolean {
        val lang = getLangCode(language)
        val targetDir = File(context.filesDir, "vosk/$lang")
        targetDir.mkdirs()

        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        // Strip leading root folder name from zip if present
                        val entryName = entry.name.substringAfter("/")
                        if (entryName.isNotBlank()) {
                            val outFile = File(targetDir, entryName)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { out ->
                                    val buf = ByteArray(32 * 1024)
                                    var len: Int
                                    while (zip.read(buf).also { len = it } != -1) {
                                        out.write(buf, 0, len)
                                    }
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            return isValidModelDirectory(targetDir)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract model zip for '$lang': ${e.message}", e)
            return false
        }
    }
}
