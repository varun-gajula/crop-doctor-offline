package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.DiagnosisOutcome
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
        val normalizedHint = cropHint?.lowercase()?.trim()

        if (normalizedHint == "cant_tell") {
            return ClassifierResult(
                classId = "unknown",
                displayName = "Not sure - ask an extension officer",
                confidence = 0.28f,
                severityBand = SeverityBand.MILD,
                certaintyBand = CertaintyBand.NOT_SURE,
                lesionFraction = 0.0f,
                inferenceTimeMs = 35L,
                memoryUsedMb = 60L,
                isDemoMode = true,
                top3 = listOf(
                    ScoredClass("weeds_not_a_crop", "Weeds Not A Crop", 0.28f),
                    ScoredClass("unknown", "Unknown", 0.22f)
                ),
                selectedCrop = null,
                cropMass = 0.12f,
                outcome = DiagnosisOutcome.CANT_TELL,
                outcomeReason = "LOW_INCROP_CONFIDENCE"
            )
        }

        if (normalizedHint == "out_of_class") {
            return ClassifierResult(
                classId = "out_of_class",
                displayName = "Out of class - plant does not match",
                confidence = 0.35f,
                severityBand = SeverityBand.MILD,
                certaintyBand = CertaintyBand.NOT_SURE,
                lesionFraction = 0.0f,
                inferenceTimeMs = 40L,
                memoryUsedMb = 60L,
                isDemoMode = true,
                top3 = listOf(
                    ScoredClass("tomato_yellow_leaf_curl_virus", "Tomato Yellow Leaf Curl Virus", 0.35f),
                    ScoredClass("tomato_healthy", "Tomato Healthy", 0.30f)
                ),
                selectedCrop = "tomato",
                cropMass = 0.22f,
                outcome = DiagnosisOutcome.OUT_OF_CLASS,
                outcomeReason = "LOW_CROP_MASS(0.22 < 0.40)"
            )
        }

        val classId = when (normalizedHint) {
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
            cropMass = 0.96f,
            outcome = DiagnosisOutcome.DIAGNOSE,
            outcomeReason = "ACCEPTED"
        )
    }

    override fun close() {}
}
