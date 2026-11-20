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

/**
 * Task P4: Three Clear Outcomes decided in plain code with measured thresholds.
 */
enum class DiagnosisOutcome {
    DIAGNOSE,
    CANT_TELL,
    OUT_OF_CLASS
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
    val top3: List<ScoredClass> = emptyList(),
    val selectedCrop: String? = null,
    val cropMass: Float = 1.0f,
    val outcome: DiagnosisOutcome = DiagnosisOutcome.DIAGNOSE,
    val outcomeReason: String = "",
    val margin: Float = 0.0f,
    val freeEnergy: Double = 0.0
)
