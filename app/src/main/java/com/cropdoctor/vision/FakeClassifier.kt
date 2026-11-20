package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.ScoredClass
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
            "chilli" -> "chilli_leaf_curl_virus"
            "tomato" -> "tomato_yellow_leaf_curl_virus"
            "maize" -> "maize_common_rust"
            else -> "rice_bacterial_leaf_blight"
        }

        val displayName = when (classId) {
            "rice_bacterial_leaf_blight" -> "Rice Bacterial Leaf Blight"
            "cotton_bacterial_blight" -> "Cotton Bacterial Blight"
            "chilli_leaf_curl_virus" -> "Chilli Leaf Curl Virus"
            "tomato_yellow_leaf_curl_virus" -> "Tomato Yellow Leaf Curl Virus"
            "maize_common_rust" -> "Maize Common Rust"
            else -> "Rice Bacterial Leaf Blight"
        }

        val top3 = listOf(
            ScoredClass(classId, displayName, 0.88f),
            ScoredClass("${cropHint ?: "rice"}_healthy", "Healthy Leaf", 0.08f),
            ScoredClass("unknown", "Other Condition", 0.04f)
        )

        return ClassifierResult(
            classId = classId,
            displayName = displayName,
            confidence = 0.88f,
            severityBand = SeverityBand.MODERATE,
            certaintyBand = CertaintyBand.SURE,
            lesionFraction = 0.15f,
            inferenceTimeMs = 45L,
            memoryUsedMb = 60L,
            isDemoMode = true,
            top3 = top3,
            selectedCrop = cropHint,
            cropMass = 0.96f
        )
    }

    override fun close() {}
}
