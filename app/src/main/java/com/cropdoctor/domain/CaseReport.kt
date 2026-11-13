package com.cropdoctor.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Structured Case Report model representing an on-device diagnosis record.
 * R8 & T3: Contains crop, label, severity, certainty (in words), date, region, photo, and farmer note.
 */
data class CaseReport(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val crop: String,
    val problemLabel: String,
    val certaintyWord: String, // "Sure", "Probably", "Not sure"
    val severity: String,      // "Mild", "Moderate", "Severe", or "N/A"
    val region: String,
    val photoPath: String? = null,
    val note: String = "",
    val isUncertain: Boolean = false,
    val classId: String = ""
)

/**
 * CaseReportRepository provides local offline persistence for Case Reports (JSON file storage in filesDir)
 * and handles the Android share sheet integration (zero INTERNET permission).
 */
object CaseReportRepository {

    private const val TAG = "CaseReportRepository"
    private const val FILE_NAME = "case_reports.json"
    private const val PHOTOS_DIR = "case_photos"
    private const val MAX_CASES = 100

    @Synchronized
    fun saveCaseReport(
        context: Context,
        crop: String,
        problemLabel: String,
        certaintyWord: String,
        severity: String,
        region: String,
        note: String = "",
        bitmap: Bitmap? = null,
        isUncertain: Boolean = false,
        classId: String = ""
    ): CaseReport {
        val id = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateString = sdf.format(Date(timestamp))

        // Save photo thumbnail locally if provided
        var savedPhotoPath: String? = null
        if (bitmap != null) {
            try {
                val photosDir = File(context.filesDir, PHOTOS_DIR)
                if (!photosDir.exists()) photosDir.mkdirs()
                val photoFile = File(photosDir, "case_${id}.jpg")
                FileOutputStream(photoFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                savedPhotoPath = photoFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save case photo locally", e)
            }
        }

        val report = CaseReport(
            id = id,
            timestamp = timestamp,
            dateString = dateString,
            crop = crop,
            problemLabel = problemLabel,
            certaintyWord = certaintyWord,
            severity = severity,
            region = region,
            photoPath = savedPhotoPath,
            note = note.trim(),
            isUncertain = isUncertain,
            classId = classId
        )

        val currentList = getAllCaseReports(context).toMutableList()
        currentList.add(0, report) // Newest first

        if (currentList.size > MAX_CASES) {
            // Delete oldest photo if present
            val removed = currentList.removeAt(currentList.size - 1)
            removed.photoPath?.let { File(it).delete() }
        }

        saveListToFile(context, currentList)
        return report
    }

    @Synchronized
    fun getAllCaseReports(context: Context): List<CaseReport> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CaseReport>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CaseReport(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        dateString = obj.optString("dateString", ""),
                        crop = obj.optString("crop", "Crop"),
                        problemLabel = obj.optString("problemLabel", "Problem"),
                        certaintyWord = obj.optString("certaintyWord", "Not sure"),
                        severity = obj.optString("severity", "N/A"),
                        region = obj.optString("region", "India"),
                        photoPath = obj.optString("photoPath", "").ifEmpty { null },
                        note = obj.optString("note", ""),
                        isUncertain = obj.optBoolean("isUncertain", false),
                        classId = obj.optString("classId", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read case reports from file", e)
            emptyList()
        }
    }

    @Synchronized
    fun deleteCaseReport(context: Context, id: String) {
        val currentList = getAllCaseReports(context).toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val removed = currentList.removeAt(index)
            removed.photoPath?.let { File(it).delete() }
            saveListToFile(context, currentList)
        }
    }

    @Synchronized
    fun clearAll(context: Context) {
        try {
            val currentList = getAllCaseReports(context)
            for (report in currentList) {
                report.photoPath?.let { File(it).delete() }
            }
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all case reports", e)
        }
    }

    private fun saveListToFile(context: Context, list: List<CaseReport>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("dateString", item.dateString)
                    put("crop", item.crop)
                    put("problemLabel", item.problemLabel)
                    put("certaintyWord", item.certaintyWord)
                    put("severity", item.severity)
                    put("region", item.region)
                    put("photoPath", item.photoPath ?: "")
                    put("note", item.note)
                    put("isUncertain", item.isUncertain)
                    put("classId", item.classId)
                }
                array.put(obj)
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write case reports to file", e)
        }
    }

    /**
     * Formats a clean, text-based case report summary suitable for SMS, WhatsApp, or local notes.
     * Complies with Rule H1 (zero INTERNET permission).
     */
    fun formatShareText(report: CaseReport): String {
        val sb = StringBuilder()
        sb.appendLine("🌾 CROP DOCTOR - CASE REPORT")
        sb.appendLine("📅 Date: ${report.dateString}")
        sb.appendLine("📍 Region: ${report.region}")
        sb.appendLine("🌱 Crop: ${report.crop}")
        sb.appendLine("🔍 Finding: ${report.problemLabel}")
        sb.appendLine("📊 Certainty: ${report.certaintyWord}")
        if (!report.isUncertain && report.severity != "N/A" && report.severity.isNotBlank()) {
            sb.appendLine("⚠️ Severity: ${report.severity}")
        }
        if (report.note.isNotBlank()) {
            sb.appendLine("📝 Farmer Note: ${report.note}")
        }
        if (report.isUncertain || report.severity.equals("Severe", ignoreCase = true)) {
            sb.appendLine("📞 Extension Contact: Kisan Call Centre (1800-180-1551)")
        }
        sb.append("🛡️ 100% Offline AI Diagnosis (ICAR / SAU Grounded)")
        return sb.toString()
    }

    /**
     * Opens the standard Android system share sheet.
     * Zero permissions required.
     */
    fun shareCaseReport(context: Context, report: CaseReport) {
        try {
            val text = formatShareText(report)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Crop Doctor Case Report - ${report.crop} (${report.problemLabel})")
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Case Report"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Android share sheet", e)
        }
    }
}

