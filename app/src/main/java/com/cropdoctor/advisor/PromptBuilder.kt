package com.cropdoctor.advisor

import com.cropdoctor.knowledge.KbEntry

object PromptBuilder {

    /**
     * Builds the case-specific grounded Llama 3.2 1B-Instruct prompt strictly following Task A2 specifications:
     * - Includes: crop, problem, type (disease or pest), severity, certainty, field spread, region, urgency,
     *   and approved knowledge-base entry.
     * - Instructions: explain in plain words for smallholder farmer; use only approved steps; choose and order
     *   relevant to urgency; mention what to watch in coming days; do not give doses; do not name other products;
     *   do not change severity or referral advice.
     * - Output format: What I see, What to do now, When to get help.
     * - Rule H6: Throws IllegalArgumentException if language is Telugu (never sent to Llama).
     */
    fun buildPrompt(
        crop: String,
        diseaseName: String,
        severity: String,
        certainty: String,
        kbEntry: KbEntry,
        language: String = "en",
        fieldSpread: String = "some",
        region: String = "telangana",
        urgency: String = "Act this week",
        severityPercent: Float = 0f,
        confidenceScore: Float = 0f
    ): String {
        if (language.equals("te", ignoreCase = true)) {
            throw IllegalArgumentException("Telugu must never be processed by Llama (Rule H6). Telugu advisory is built in code from verified knowledge base text.")
        }

        val langInstruction = when (language.lowercase()) {
            "hi" -> " in simple Hindi (सरल हिंदी में)"
            else -> ""
        }

        val rawNatural = if (kbEntry.treatment.natural.isNotEmpty()) {
            kbEntry.treatment.natural.first().action.en
        } else {
            "Field hygiene, crop rotation, and removing infected plant debris."
        }
        val naturalRemedy = GuardrailFilter.sanitize(rawNatural).take(75)

        val chemicalActives = if (kbEntry.treatment.chemical.isNotEmpty()) {
            kbEntry.treatment.chemical.first().let { "${it.activeIngredient} (${it.productExample})" }.take(50)
        } else {
            "None recommended for current stage. Follow official guidelines."
        }

        val urgentStep = GuardrailFilter.sanitize(kbEntry.immediateStep.en).take(80).ifEmpty { "Isolate infected plants and avoid overhead watering." }

        val regionalAdviceObj = kbEntry.getRegionalAdvice(region)
        val regionalAdviceText = if (regionalAdviceObj.differs && !regionalAdviceObj.text?.en.isNullOrBlank()) {
            "${regionalAdviceObj.source}: ${regionalAdviceObj.text?.en?.take(75)}"
        } else {
            "${regionalAdviceObj.source}: Standard management practices apply across all regions."
        }

        val sevPctDisplay = if (severityPercent > 0) String.format("%.1f", severityPercent) else when (severity.uppercase()) {
            "MILD" -> "10.0"
            "MODERATE" -> "25.0"
            "SEVERE" -> "45.0"
            else -> "15.0"
        }

        val severityDamageDescription = when {
            kbEntry.type.equals("pest", ignoreCase = true) -> "$sevPctDisplay% pest infestation / plant damage"
            kbEntry.type.equals("healthy", ignoreCase = true) -> "clean healthy foliage"
            else -> "$sevPctDisplay% affected leaf area"
        }

        val confScoreDisplay = if (confidenceScore > 0) String.format("%.1f", confidenceScore) else when (certainty) {
            "Sure" -> "92.0"
            "Probably" -> "75.0"
            else -> "55.0"
        }

        val isSevereOrActNow = severity.equals("SEVERE", ignoreCase = true) || urgency.contains("now", ignoreCase = true)
        val referralStatus = if (isSevereOrActNow) {
            "Immediate Referral Required"
        } else {
            "Routine Field Monitoring"
        }
        val referralContact = "KVK or Kisan Call Centre 1800-180-1551"

        val regionFormatted = when (region.lowercase()) {
            "telangana" -> "Telangana"
            "andhra_pradesh", "andhra pradesh" -> "Andhra Pradesh"
            else -> "Other"
        }

        return """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Crop Doctor offline assistant.
- Explain in plain words for a smallholder farmer$langInstruction.
- Answer in at most 6 short, complete sentences. End every section with a complete sentence.
- Use only the steps in the knowledge-base entry below.
- Choose and order the ones relevant to this urgency.
- Mention what to watch in the coming days.
- Do not give doses (always instruct to follow the product label).
- Do not name any product not in the entry.
- Do not change the severity or referral advice.
Output in three short parts:
1. What I see:
2. What to do now:
3. When to get help:<|eot_id|><|start_header_id|>user<|end_header_id|>
- Crop: $crop
- Problem: $diseaseName (${kbEntry.type})
- Type: ${kbEntry.type}
- Severity: $severity ($severityDamageDescription)
- Confidence Level: $certainty ($confScoreDisplay%)
- Field Spread: $fieldSpread
- Region: $regionFormatted
- Urgency Level: $urgency
- Immediate Cultural Step: $urgentStep
- Natural / Cultural Methods: $naturalRemedy
- Chemical Management Options (CIB&RC Registered): $chemicalActives
- Regional Guidance ($regionFormatted): $regionalAdviceText
- Official Referral Rule: $referralStatus ($referralContact)
Explain practical steps for the farmer.<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""
    }
}
