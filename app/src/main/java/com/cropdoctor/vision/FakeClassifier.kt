package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.SeverityBand

/**
 * Fake classifier returning fixed sample results for testing and demo runs.
 * Returns deterministic sample predictions marked with isDemoMode = true,
 * which displays a clear banner "DEMO MODE - NOT A REAL DIAGNOSIS" in the UI.
 */
class FakeClassifier : Classifier {

    override suspend fun classify(bitmap: Bitmap): ClassifierResult {
        return sampleResult(null)
    }

    override suspend fun classify(bitmap: Bitmap, cropHint: String?): ClassifierResult {
        return sampleResult(cropHint)
    }

    private fun sampleResult(cropHint: String?): ClassifierResult {
        val classId = when (cropHint?.lowercase()) {
            "rice" -> "rice_bacterial_leaf_blight"
            "cotton" -> "cotton_bacterial_blight"
            "chilli" -> "chilli_leaf_curl"
            "tomato" -> "tomato_early_blight"
            "maize" -> "maize_fall_armyworm"
            else -> "rice_bacterial_leaf_blight"
        }

        val displayName = when (classId) {
            "rice_bacterial_leaf_blight" -> "Rice Bacterial Leaf Blight"
            "cotton_bacterial_blight" -> "Cotton Bacterial Blight"
            "chilli_leaf_curl" -> "Chilli Leaf Curl"
            "tomato_early_blight" -> "Tomato Early Blight"
            "maize_fall_armyworm" -> "Maize Fall Armyworm"
            else -> "Rice Bacterial Leaf Blight"
        }

        return ClassifierResult(
            classId = classId,
            displayName = displayName,
            confidence = 0.88f,
            severityBand = SeverityBand.MODERATE,
            certaintyBand = CertaintyBand.SURE,
            lesionFraction = 0.22f,
            inferenceTimeMs = 42L,
            memoryUsedMb = getProcessMemoryMb(),
            isDemoMode = true
        )
    }

    private fun getProcessMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    override fun close() {
        // No resources to release
    }
}
