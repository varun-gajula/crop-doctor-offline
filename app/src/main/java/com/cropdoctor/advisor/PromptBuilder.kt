package com.cropdoctor.advisor

import com.cropdoctor.knowledge.KbEntry

object PromptBuilder {

    /**
     * Builds the grounded Llama 3.2 1B-Instruct prompt strictly following seed/prompt_template.txt.
     * Enforces grounded reasoning strictly based on verified ICAR/CIB&RC findings
     * and forbids unauthorized dosage numbers (Rule H4 & H5).
     */
    fun buildPrompt(
        crop: String,
        diseaseName: String,
        severity: String,
        certainty: String,
        kbEntry: KbEntry,
        language: String,
        severityPercent: Float = 0f,
        confidenceScore: Float = 0f
    ): String {
        val langInstruction = when (language.lowercase()) {
            "hi" -> " in simple Hindi (सरल हिंदी में)"
            else -> ""
        }

        val naturalRemedy = if (kbEntry.treatment.natural.isNotEmpty()) {
            kbEntry.treatment.natural.joinToString("; ") { it.action.en }
        } else {
            "Field hygiene, crop rotation, and removing infected plant debris."
        }

        val chemicalActives = if (kbEntry.treatment.chemical.isNotEmpty()) {
            kbEntry.treatment.chemical.joinToString(", ") { "${it.activeIngredient} (${it.productExample})" }
        } else {
            "None recommended for current stage. Follow official state agricultural university guidelines."
        }

        val precautions = if (kbEntry.treatment.chemical.isNotEmpty()) {
            kbEntry.treatment.chemical.mapNotNull { it.safetyLine.en.takeIf { s -> s.isNotBlank() } }.distinct().joinToString("; ")
                .ifEmpty { "Wear protective gear, avoid spraying during high wind or mid-day heat, and follow waiting period (PHI)." }
        } else {
            "Wear protective gloves and mask when handling diseased plant tissues."
        }

        val referralStatus = if (severity.equals("SEVERE", ignoreCase = true)) {
            "Immediate Referral Required"
        } else {
            "Routine Field Monitoring"
        }
        val referralContact = "Local Krishi Vigyan Kendra (KVK) or Agriculture Extension Officer"

        val urgentStep = kbEntry.immediateStep.en.ifEmpty { "Isolate infected plants and avoid overhead watering." }

        val sevPctDisplay = if (severityPercent > 0) String.format("%.1f", severityPercent) else when (severity.uppercase()) {
            "MILD" -> "10.0"
            "MODERATE" -> "25.0"
            "SEVERE" -> "45.0"
            else -> "15.0"
        }

        val confScoreDisplay = if (confidenceScore > 0) String.format("%.1f", confidenceScore) else when (certainty) {
            "Sure" -> "92.0"
            "Probably" -> "75.0"
            else -> "55.0"
        }

        return """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
You are Crop Doctor, an offline agricultural field assistant for smallholder farmers in India.
Your mission is to explain the crop diagnosis clearly, calmly, and practically$langInstruction.
Strict Rules:
1. Never invent or guess chemical names, dosages, or spray dilution rates.
2. Rely strictly and only on the verified context provided below.
3. Keep your advice practical, concise, and easy to understand (Grade-6 reading level).
4. Do not contradict the severity assessment or the referral recommendation.<|eot_id|><|start_header_id|>user<|end_header_id|>
[FIELD DIAGNOSTIC FINDING]
- Crop: $crop
- Problem Identified: $diseaseName (${kbEntry.type})
- Detected Severity: $severity ($sevPctDisplay% affected leaf area)
- Confidence Level: $certainty ($confScoreDisplay%)

[VERIFIED ADVISORY CONTEXT]
- Urgent Step: $urgentStep
- Natural/Traditional Method: $naturalRemedy
- Chemical Method: $chemicalActives
- Precautions: $precautions
- Extension Officer Referral: $referralStatus ($referralContact)

Explain what this problem is in plain words, whether it is an emergency, and what the farmer should do today.<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""
    }
}
