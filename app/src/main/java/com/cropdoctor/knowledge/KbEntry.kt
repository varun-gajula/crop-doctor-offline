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
data class RegionalAdvice(
    val source: String,
    val differs: Boolean = false,
    val text: LocalizedText? = null
)

@Serializable
data class KbEntry(
    @SerialName("class_id") val classId: String,
    val index: Int,
    val crop: String,
    val type: String,
    val pathogen: String,
    val names: LocalizedText,
    val stage: String? = null,
    @SerialName("needs_expert_review") val needsExpertReview: Boolean = true,
    @SerialName("referral_threshold") val referralThreshold: String = "SEVERE",
    @SerialName("severity_advice") val severityAdvice: Map<String, LocalizedText> = emptyMap(),
    @SerialName("immediate_step") val immediateStep: LocalizedText,
    @SerialName("explain_text") val explainText: LocalizedText,
    @SerialName("prevention_and_field_practice") val preventionAndFieldPractice: LocalizedText = LocalizedText(),
    @SerialName("early_warning_signs") val earlyWarningSigns: LocalizedText = LocalizedText(),
    @SerialName("when_to_call_extension_officer") val whenToCallExtensionOfficer: LocalizedText = LocalizedText(),
    val treatment: TreatmentPlan = TreatmentPlan(),
    val regional: Map<String, RegionalAdvice> = emptyMap()
) {
    fun getRegionalAdvice(region: String): RegionalAdvice {
        val key = when (region.lowercase().replace(" ", "_")) {
            "telangana" -> "telangana"
            "andhra_pradesh", "andhra" -> "andhra_pradesh"
            else -> "other"
        }
        return regional[key] ?: RegionalAdvice(
            source = when (key) {
                "telangana" -> "PJTSAU"
                "andhra_pradesh" -> "ANGRAU"
                else -> "ICAR"
            },
            differs = false
        )
    }
}

@Serializable
data class KnowledgeBaseRoot(
    val version: String,
    @SerialName("total_classes") val totalClasses: Int,
    val classes: List<KbEntry>
)

