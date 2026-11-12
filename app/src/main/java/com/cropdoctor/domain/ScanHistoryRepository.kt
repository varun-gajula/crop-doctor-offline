package com.cropdoctor.domain

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
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

object ScanHistoryRepository {

    private const val PREF_NAME = "crop_doctor_scan_history"
    private const val KEY_HISTORY = "history_records"
    private const val MAX_RECORDS = 50

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveScan(
        context: Context,
        crop: String,
        classId: String,
        diseaseName: String,
        confidence: Float,
        severity: String
    ) {
        try {
            val list = getHistory(context).toMutableList()
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date())

            val record = ScanHistoryRecord(
                id = System.currentTimeMillis().toString(),
                crop = crop.replaceFirstChar { it.uppercase() },
                classId = classId,
                diseaseName = diseaseName,
                confidence = confidence,
                severity = severity,
                timestamp = System.currentTimeMillis(),
                dateString = dateStr
            )

            // Insert at beginning (newest first)
            list.add(0, record)
            if (list.size > MAX_RECORDS) {
                list.removeAt(list.size - 1)
            }

            // Serialize
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("crop", item.crop)
                    put("classId", item.classId)
                    put("diseaseName", item.diseaseName)
                    put("confidence", item.confidence.toDouble())
                    put("severity", item.severity)
                    put("timestamp", item.timestamp)
                    put("dateString", item.dateString)
                }
                jsonArray.put(obj)
            }

            getPrefs(context).edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHistory(context: Context): List<ScanHistoryRecord> {
        val jsonStr = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        val results = mutableListOf<ScanHistoryRecord>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                results.add(
                    ScanHistoryRecord(
                        id = obj.optString("id", i.toString()),
                        crop = obj.optString("crop", "Crop"),
                        classId = obj.optString("classId", ""),
                        diseaseName = obj.optString("diseaseName", "Leaf Scan"),
                        confidence = obj.optDouble("confidence", 0.9).toFloat(),
                        severity = obj.optString("severity", "MILD"),
                        timestamp = obj.optLong("timestamp", 0L),
                        dateString = obj.optString("dateString", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
    }
}

