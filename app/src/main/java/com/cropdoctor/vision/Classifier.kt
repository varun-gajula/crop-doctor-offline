package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.ClassifierResult

interface Classifier {
    suspend fun classify(bitmap: Bitmap): ClassifierResult
    suspend fun classify(bitmap: Bitmap, cropHint: String?): ClassifierResult = classify(bitmap)
    fun close()
}
