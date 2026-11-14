package com.cropdoctor.domain

enum class SeverityBand {
    MILD,
    MODERATE,
    SEVERE
}

enum class CertaintyBand {
    SURE,       // confidence >= 0.80
    PROBABLY,   // 0.60 <= confidence < 0.80
    NOT_SURE    // confidence < 0.60
}

data class ScoredClass(
    val classId: String,
    val displayName: String,
    val probability: Float
)

data class ClassifierResult(
    val classId: String,
    val displayName: String,
    val confidence: Float,
    val severityBand: SeverityBand,
    val certaintyBand: CertaintyBand,
    val lesionFraction: Float,
    val inferenceTimeMs: Long = 0L,
    val memoryUsedMb: Long = 0L,
    val isDemoMode: Boolean = false,
    val top3: List<ScoredClass> = emptyList()
)
