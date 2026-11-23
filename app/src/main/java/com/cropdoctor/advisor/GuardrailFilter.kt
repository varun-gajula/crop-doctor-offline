package com.cropdoctor.advisor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import java.util.regex.Pattern

data class GuardrailResult(
    val isValid: Boolean,
    val sanitizedText: String,
    val failureReason: String? = null
)

object GuardrailFilter {

    // Regex detecting unverified dosage numbers and rates (e.g. 50 ml/L, 5-10 g/L, 2 kg per acre, 2.5 g/l, 2.5 ml/L of water)
    private val DOSE_PATTERN = Pattern.compile(
        "\\(?(?:@\\s*)?\\b\\d+(?:-\\d+)?(?:\\.\\d+)?\\s*(?:ml|g|gm|mg|l|litre|liter|kg)\\s*(?:per|/|in)?\\s*(?:\\d*\\s*)?(?:l|litre|liter|acre|ha|hectare|water)?(?:\\s+of\\s+water)?\\)?",
        Pattern.CASE_INSENSITIVE
    )

    // General dose pattern detecting numeric quantities with chemical/spray measurement units
    private val GENERAL_DOSE_PATTERN = Pattern.compile(
        "\\b\\d+(?:-\\d+)?(?:\\.\\d+)?\\s*(?:ml|g|gm|mg)\\s*(?:/|per|in)?\\s*(\\w+)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val REPLACEMENT = "follow the product label"

    // Recognized agrochemicals to protect against unauthorized hallucinated chemical advice
    private val KNOWN_AGROCHEMICALS = listOf(
        "carbendazim", "chlorpyrifos", "monocrotophos", "glyphosate", "imidacloprid",
        "mancozeb", "propiconazole", "hexaconazole", "acephate", "lambda-cyhalothrin",
        "cartap hydrochloride", "tricyclazole", "copper oxychloride", "streptocycline",
        "spinosad", "emamectin benzoate", "chlorantraniliprole", "dimethoate",
        "malathion", "endosulfan", "paraquat", "atrazine", "pendimethalin",
        "metalaxyl", "tebuconazole", "azoxystrobin", "difenoconazole", "cypermethrin",
        "deltamethrin", "thiamethoxam", "fipronil", "profenofos", "diafenthiuron"
    )

    private val SPECIAL_TOKEN_REGEX = Regex("<\\|[a-zA-Z0-9_]+\\|>")

    fun sanitize(text: String): String {
        val noSpecial = SPECIAL_TOKEN_REGEX.replace(text, "")
        val matcher = DOSE_PATTERN.matcher(noSpecial)
        return matcher.replaceAll(REPLACEMENT)
    }

    fun hasDose(text: String): Boolean {
        return DOSE_PATTERN.matcher(text).find() || GENERAL_DOSE_PATTERN.matcher(text).find()
    }

    /**
     * Comprehensive Guardrail validation:
     * - Rejects any dose numbers (Rule H4).
     * - Rejects any unknown chemical/pesticide active ingredients not in the official KB entry (Rule H4).
     * - Rejects contradictions with the diagnosed severity band (Rule H5).
     * - Rejects contradictions with referral advice from RulesEngine (Task A2 Requirement 3).
     * If validation fails, caller MUST discard AI output and display the verified KB text alone ("Offline guide").
     */
    fun validateAdvice(
        generatedText: String,
        kbEntry: KbEntry,
        severityBand: SeverityBand,
        urgencyLevel: RulesEngine.UrgencyLevel? = null,
        certaintyBand: CertaintyBand = CertaintyBand.SURE
    ): GuardrailResult {
        // 1. Check for dosage numbers
        if (hasDose(generatedText)) {
            return GuardrailResult(
                isValid = false,
                sanitizedText = sanitize(generatedText),
                failureReason = "Contains unverified dosage rate (violates Rule H4)"
            )
        }

        val lowerText = generatedText.lowercase()

        // 2. Check for unapproved chemical active ingredients
        val approvedChemicals = kbEntry.treatment.chemical.flatMap { chemical ->
            listOf(
                chemical.activeIngredient.lowercase(),
                chemical.productExample.lowercase()
            )
        }.filter { it.isNotBlank() }

        for (chemical in KNOWN_AGROCHEMICALS) {
            if (lowerText.contains(chemical)) {
                val isApproved = approvedChemicals.any { approved ->
                    approved.contains(chemical) || chemical.contains(approved)
                }
                if (!isApproved) {
                    return GuardrailResult(
                        isValid = false,
                        sanitizedText = generatedText,
                        failureReason = "Contains unapproved active ingredient '$chemical' not in official KB entry"
                    )
                }
            }
        }

        // 3. Check for severity contradiction
        when (severityBand) {
            SeverityBand.MILD -> {
                val severeContradictions = listOf(
                    "severe emergency",
                    "destroy all plants",
                    "destroy the entire field",
                    "catastrophic failure",
                    "emergency spray immediately"
                )
                if (severeContradictions.any { lowerText.contains(it) }) {
                    return GuardrailResult(
                        isValid = false,
                        sanitizedText = generatedText,
                        failureReason = "Contradicts MILD severity assessment by declaring an extreme emergency"
                    )
                }
            }
            SeverityBand.SEVERE -> {
                val mildContradictions = listOf(
                    "harmless condition",
                    "no action needed",
                    "ignore this symptom",
                    "do not worry, it will go away"
                )
                if (mildContradictions.any { lowerText.contains(it) }) {
                    return GuardrailResult(
                        isValid = false,
                        sanitizedText = generatedText,
                        failureReason = "Contradicts SEVERE severity assessment by claiming no action is needed"
                    )
                }
            }
            SeverityBand.MODERATE -> {
                // Moderate is balanced
            }
        }

        // 4. Check for referral contradiction (Task A2 Requirement 3)
        val referralRequired = (urgencyLevel == RulesEngine.UrgencyLevel.ACT_NOW) ||
                (severityBand == SeverityBand.SEVERE) ||
                (certaintyBand == CertaintyBand.NOT_SURE) ||
                RulesEngine.shouldReferToKVK(severityBand, kbEntry.referralThreshold)

        if (referralRequired) {
            val antiReferralPhrases = listOf(
                "no need to call",
                "do not call",
                "do not contact",
                "no extension officer needed",
                "no extension worker needed",
                "no help needed",
                "do not consult",
                "harmless condition",
                "ignore this symptom"
            )
            if (antiReferralPhrases.any { lowerText.contains(it) }) {
                return GuardrailResult(
                    isValid = false,
                    sanitizedText = generatedText,
                    failureReason = "Contradicts referral advice: recommends against contacting extension officer when referral is required"
                )
            }
        } else {
            val falseEmergencyPhrases = listOf(
                "severe emergency",
                "emergency referral required immediately",
                "call extension officer immediately",
                "destroy all plants",
                "catastrophic failure",
                "destroy the entire field"
            )
            if (falseEmergencyPhrases.any { lowerText.contains(it) }) {
                return GuardrailResult(
                    isValid = false,
                    sanitizedText = generatedText,
                    failureReason = "Contradicts non-emergency advice: claims extreme emergency when routine monitoring is advised"
                )
            }
        }

        return GuardrailResult(
            isValid = true,
            sanitizedText = sanitize(generatedText),
            failureReason = null
        )
    }
}
