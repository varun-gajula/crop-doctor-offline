package com.cropdoctor.advisor

import com.cropdoctor.knowledge.KbEntry

object PromptBuilder {

    /**
     * Builds the grounded Llama 3.2 1B-Instruct prompt.
     * The prompt enforces grounded reasoning strictly based on verified ICAR/CIB&RC findings
     * and forbids unauthorized dosage numbers.
     */
    fun buildPrompt(
        crop: String,
        diseaseName: String,
        severity: String,
        certainty: String,
        kbEntry: KbEntry,
        language: String
    ): String {
        val langName = when (language.lowercase()) {
            "hi" -> "Hindi (हिंदी)"
            else -> "English"
        }

        val naturalSteps = kbEntry.treatment.natural.joinToString("; ") { it.action.en }
        val chemicalActives = kbEntry.treatment.chemical.joinToString(", ") { it.activeIngredient }

        return """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
You are an expert Indian agricultural extension specialist. 
Your role is to explain crop diagnosis clearly and safely to Indian smallholder farmers.
RULES:
1. Base your explanation strictly on the provided official findings.
2. Answer in $langName.
3. NEVER state specific chemical dosages or units (e.g. ml/L, kg/ha). Always say "follow the product label".
4. Keep explanation concise (under 80 words) and practical.<|eot_id|>
<|start_header_id|>user<|end_header_id|>
Crop: $crop
Diagnosis: $diseaseName
Severity: $severity
Confidence: $certainty
Official Finding: ${kbEntry.explainText.en}
Recommended Natural Practice: $naturalSteps
Approved Chemical Actives: $chemicalActives

Explain what is happening to the plant, provide the most urgent action, and state when to consult the local agriculture officer.<|eot_id|>
<|start_header_id|>assistant<|end_header_id|>
""".trimIndent()
    }
}

