package com.cropdoctor.vision

import android.graphics.Bitmap
import com.cropdoctor.domain.ClassifierResult

data class ImageAnalysisMetadata(
    val source: String = "camera", // "camera" or "gallery"
    val origWidth: Int = 0,
    val origHeight: Int = 0,
    val exifOrientation: Int = 0,
    val leafValidatorPass: Boolean = true,
    val leafValidatorFraction: Float = 1.0f,
    val leafValidatorReason: String = "OK"
)

interface Classifier {
    suspend fun classify(bitmap: Bitmap): ClassifierResult
    suspend fun classify(bitmap: Bitmap, cropHint: String?): ClassifierResult = classify(bitmap)
    suspend fun classify(bitmap: Bitmap, cropHint: String?, metadata: ImageAnalysisMetadata?): ClassifierResult = classify(bitmap, cropHint)
    fun close()
}
