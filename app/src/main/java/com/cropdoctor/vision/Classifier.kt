package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.ClassifierResult

interface Classifier {
    suspend fun classify(bitmap: Bitmap): ClassifierResult
    fun close()
}
