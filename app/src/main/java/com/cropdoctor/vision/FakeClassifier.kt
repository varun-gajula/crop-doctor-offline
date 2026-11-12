package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.ClassifierResult

/**
 * Fallback classifier delegating directly to SmartVisualClassifier.
 * Accurately analyzes real leaf features dynamically across all 25 classes.
 */
class FakeClassifier : Classifier {

    private val smartClassifier = SmartVisualClassifier()

    override suspend fun classify(bitmap: Bitmap): ClassifierResult {
        return smartClassifier.classify(bitmap, null)
    }

    override suspend fun classify(bitmap: Bitmap, cropHint: String?): ClassifierResult {
        return smartClassifier.classify(bitmap, cropHint)
    }

    override fun close() {
        smartClassifier.close()
    }
}
