package com.cropdoctor.domain

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanHistoryRecord(
    val id: String,
    val crop: String,
    val classId: String,
    val diseaseName: String,
    val confidence: Float,
    val severity: String,
    val timestamp: Long,
    val dateString: String
)

/**
 * T3: ScanHistoryRepository merged with CaseReportRepository.
 * CaseReportRepository is the single source of truth for both Scan History and Case Reports.
 */
object ScanHistoryRepository {

    fun saveScan(
        context: Context,
        crop: String,
        classId: String,
        diseaseName: String,
        confidence: Float,
        severity: String
    ) {
        val certainty = RulesEngine.calculateCertainty(confidence)
        val certaintyWord = when (certainty) {
            CertaintyBand.SURE -> "Sure"
            CertaintyBand.PROBABLY -> "Probably"
            CertaintyBand.NOT_SURE -> "Not sure"
        }
        val isUncertain = certainty == CertaintyBand.NOT_SURE || classId == "unknown"

        CaseReportRepository.saveCaseReport(
            context = context,
            crop = crop.replaceFirstChar { it.uppercase() },
            problemLabel = diseaseName,
            certaintyWord = certaintyWord,
            severity = if (isUncertain) "N/A" else severity,
            region = "India",
            note = "",
            bitmap = null,
            isUncertain = isUncertain,
            classId = classId
        )
    }

    fun getHistory(context: Context): List<ScanHistoryRecord> {
        return CaseReportRepository.getAllCaseReports(context).map { case ->
            val conf = when (case.certaintyWord.lowercase()) {
                "sure" -> 0.88f
                "probably" -> 0.65f
                else -> 0.0f
            }
            ScanHistoryRecord(
                id = case.id,
                crop = case.crop,
                classId = case.classId,
                diseaseName = case.problemLabel,
                confidence = conf,
                severity = case.severity,
                timestamp = case.timestamp,
                dateString = case.dateString
            )
        }
    }

    fun clearHistory(context: Context) {
        CaseReportRepository.clearAll(context)
    }
}

