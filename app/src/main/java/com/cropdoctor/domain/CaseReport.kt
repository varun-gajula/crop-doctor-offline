package com.cropdoctor.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.cropdoctor.advisor.GuardrailFilter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Structured Case Report model representing an on-device diagnosis record (Requirement F9).
 * Contains: crop, finding, type, severity, certainty, region, date, photo and note.
 * Must NOT contain active ingredients, doses or product names unless dose_verified is true.
 */
data class CaseReport(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val crop: String,
    val problemLabel: String,
    val type: String = "Disease", // "Disease", "Pest", "Healthy", or "N/A"
    val certaintyWord: String,    // "Sure", "Probably", "Not sure"
    val severity: String,         // "Mild", "Moderate", "Severe", or "N/A"
    val region: String,
    val photoPath: String? = null,
    val note: String = "",
    val isUncertain: Boolean = false,
    val classId: String = "",
    val doseVerified: Boolean = false,
    val activeIngredient: String? = null,
    val productExample: String? = null,
    val verifiedDose: String? = null
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
        type: String = "Disease",
        certaintyWord: String,
        severity: String,
        region: String,
        note: String = "",
        bitmap: Bitmap? = null,
        isUncertain: Boolean = false,
        classId: String = "",
        doseVerified: Boolean = false,
        activeIngredient: String? = null,
        productExample: String? = null,
        verifiedDose: String? = null
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
            type = type,
            certaintyWord = certaintyWord,
            severity = severity,
            region = region,
            photoPath = savedPhotoPath,
            note = note.trim(),
            isUncertain = isUncertain,
            classId = classId,
            doseVerified = doseVerified,
            activeIngredient = if (doseVerified) activeIngredient else null,
            productExample = if (doseVerified) productExample else null,
            verifiedDose = if (doseVerified) verifiedDose else null
        )

        val currentList = getAllCaseReports(context).toMutableList()
        currentList.add(0, report) // Newest first

        if (currentList.size > MAX_CASES) {
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
                        type = obj.optString("type", "Disease"),
                        certaintyWord = obj.optString("certaintyWord", "Not sure"),
                        severity = obj.optString("severity", "N/A"),
                        region = obj.optString("region", "India"),
                        photoPath = obj.optString("photoPath", "").ifEmpty { null },
                        note = obj.optString("note", ""),
                        isUncertain = obj.optBoolean("isUncertain", false),
                        classId = obj.optString("classId", ""),
                        doseVerified = obj.optBoolean("doseVerified", false),
                        activeIngredient = obj.optString("activeIngredient", "").ifEmpty { null },
                        productExample = obj.optString("productExample", "").ifEmpty { null },
                        verifiedDose = obj.optString("verifiedDose", "").ifEmpty { null }
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
                    put("type", item.type)
                    put("certaintyWord", item.certaintyWord)
                    put("severity", item.severity)
                    put("region", item.region)
                    put("photoPath", item.photoPath ?: "")
                    put("note", item.note)
                    put("isUncertain", item.isUncertain)
                    put("classId", item.classId)
                    put("doseVerified", item.doseVerified)
                    put("activeIngredient", item.activeIngredient ?: "")
                    put("productExample", item.productExample ?: "")
                    put("verifiedDose", item.verifiedDose ?: "")
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
     * Formats the case report text for sharing via the Android share sheet (Requirement F9).
     * It may contain crop, finding, type, severity, certainty, region, date, photo and note.
     * It must NOT contain active ingredients, doses or product names unless dose_verified is true.
     * All extraneous text not in F9 is strictly excluded.
     */
    fun formatShareText(report: CaseReport): String {
        val sb = StringBuilder()
        sb.appendLine("CROP DOCTOR - FIELD REPORT")
        sb.appendLine("Date: ${report.dateString}")
        sb.appendLine("Region: ${report.region}")
        sb.appendLine("Crop: ${report.crop}")
        sb.appendLine("Finding: ${report.problemLabel}")
        sb.appendLine("Type: ${report.type}")
        sb.appendLine("Certainty: ${report.certaintyWord}")
        if (!report.isUncertain && report.severity != "N/A" && report.severity.isNotBlank()) {
            sb.appendLine("Severity: ${report.severity}")
        }
        val photoStatus = if (!report.photoPath.isNullOrBlank()) "Attached" else "None"
        sb.appendLine("Photo: $photoStatus")
        if (report.note.isNotBlank()) {
            val cleanNote = if (!report.doseVerified) GuardrailFilter.sanitize(report.note.trim()) else report.note.trim()
            sb.appendLine("Note: $cleanNote")
        }
        // F9 & Rule H4: It must NOT contain active ingredients, doses or product names unless dose_verified is true.
        if (report.doseVerified && !report.activeIngredient.isNullOrBlank()) {
            val doseStr = report.verifiedDose?.ifBlank { "follow the product label" } ?: "follow the product label"
            val prodStr = if (!report.productExample.isNullOrBlank()) " (${report.productExample})" else ""
            sb.appendLine("Treatment: ${report.activeIngredient}$prodStr - Dose: $doseStr")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Opens the standard Android system share sheet.
     * Zero permissions required (Rule H1).
     */
    fun shareCaseReport(context: Context, report: CaseReport) {
        try {
            val text = formatShareText(report)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Crop Doctor Field Report - ${report.crop} (${report.problemLabel})")
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Field Report"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Android share sheet", e)
        }
    }
}
