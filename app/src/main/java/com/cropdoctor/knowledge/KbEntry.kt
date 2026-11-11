package com.cropdoctor.knowledge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalizedText(
    val en: String = "",
    val te: String = "",
    val hi: String = ""
) {
    fun forLanguage(lang: String): String = when (lang.lowercase()) {
        "te" -> te.ifEmpty { en }
        "hi" -> hi.ifEmpty { en }
        else -> en
    }
}

@Serializable
data class NaturalStep(
    val step: Int,
    val action: LocalizedText,
    val source: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("source_quote") val sourceQuote: String
)

@Serializable
data class ChemicalStep(
    val step: Int,
    @SerialName("active_ingredient") val activeIngredient: String,
    @SerialName("product_example") val productExample: String,
    @SerialName("dose_verified") val doseVerified: Boolean = false,
    val dose: String = "follow the product label",
    @SerialName("phi_days") val phiDays: Int? = null,
    @SerialName("safety_line") val safetyLine: LocalizedText,
    val source: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("source_quote") val sourceQuote: String
)

@Serializable
data class TreatmentPlan(
    val natural: List<NaturalStep> = emptyList(),
    val chemical: List<ChemicalStep> = emptyList()
)

@Serializable
data class KbEntry(
    @SerialName("class_id") val classId: String,
    val index: Int,
    val crop: String,
    val type: String,
    val pathogen: String,
    val names: LocalizedText,
    @SerialName("needs_expert_review") val needsExpertReview: Boolean = true,
    @SerialName("referral_threshold") val referralThreshold: String = "SEVERE",
    @SerialName("severity_advice") val severityAdvice: Map<String, LocalizedText> = emptyMap(),
    @SerialName("immediate_step") val immediateStep: LocalizedText,
    @SerialName("explain_text") val explainText: LocalizedText,
    val treatment: TreatmentPlan = TreatmentPlan()
)

@Serializable
data class KnowledgeBaseRoot(
    val version: String,
    @SerialName("total_classes") val totalClasses: Int,
    val classes: List<KbEntry>
)

