package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.CertaintyCalculator
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.SeverityCalculator
import kotlinx.coroutines.delay

/**
 * FakeClassifier enables instant emulator development, testing, and UI validation
 * without requiring the on-device compiled ExecuTorch .pte file or real camera hardware.
 */
class FakeClassifier : Classifier {

    override suspend fun classify(bitmap: Bitmap): ClassifierResult {
        // Simulate inference latency (e.g. 120ms on mobile CPU)
        delay(120)

        val classId = "rice_bacterial_leaf_blight"
        val confidence = 0.88f
        val lesionFraction = 0.22f // 22% leaf area affected

        val severityBand = SeverityCalculator.calculate(lesionFraction, classId)
        val certaintyBand = CertaintyCalculator.calculate(confidence)

        return ClassifierResult(
            classId = classId,
            displayName = "Rice Bacterial Leaf Blight",
            confidence = confidence,
            severityBand = severityBand,
            certaintyBand = certaintyBand,
            lesionFraction = lesionFraction
        )
    }

    override fun close() {
        // No-op for fake classifier
    }
}

