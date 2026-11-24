package com.cropdoctor.advisor

import com.cropdoctor.knowledge.KbEntry

object PromptBuilder {

    /**
     * Builds the case-specific grounded Llama 3.2 1B-Instruct prompt strictly following Task LAT-1 specifications:
     * - Minimizes prompt tokens to reduce time to first token (TTFT).
     * - Kept grounded: Finding, only single most relevant sentences from knowledge base, and 6-sentence instruction.
     * - Output format: 1. What I see, 2. What to do now, 3. When to get help.
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

        val isHindi = language.equals("hi", ignoreCase = true)
        val langInstruction = when (language.lowercase()) {
            "hi" -> " in simple Hindi (सरल हिंदी में)"
            else -> ""
        }

        val rawNatural = if (kbEntry.treatment.natural.isNotEmpty()) {
            val stepAction = if (isHindi) kbEntry.treatment.natural.first().action.hi else kbEntry.treatment.natural.first().action.en
            stepAction.split('.').firstOrNull { it.isNotBlank() }?.let { "$it." }
                ?: stepAction
        } else {
            if (isHindi) "खेत की स्वच्छता, फसल चक्र और अवशेषों का प्रबंधन।" else "Field hygiene, crop rotation, and sanitation."
        }
        val naturalRemedy = GuardrailFilter.sanitize(rawNatural).take(75)

        val chemicalActives = if (kbEntry.treatment.chemical.isNotEmpty()) {
            kbEntry.treatment.chemical.first().let { "${it.activeIngredient} (${it.productExample})" }.take(50)
        } else {
            if (isHindi) "वर्तमान स्थिति में किसी रासायनिक दवा की आवश्यकता नहीं है।" else "None recommended for current stage."
        }

        val rawUrgent = if (isHindi) {
            val step = kbEntry.immediateStep.hi
            step.split('।').firstOrNull { it.isNotBlank() }?.let { "$it।" } ?: step
        } else {
            val step = kbEntry.immediateStep.en
            step.split('.').firstOrNull { it.isNotBlank() }?.let { "$it." } ?: step
        }
        val urgentStep = GuardrailFilter.sanitize(rawUrgent).take(80).ifEmpty {
            if (isHindi) "संक्रमित पौधों को अलग करें और पत्तियों पर पानी का जमाव न होने दें।" else "Isolate infected plants and avoid overhead watering."
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

        val isSevereOrActNow = severity.equals("SEVERE", ignoreCase = true) || urgency.contains("now", ignoreCase = true)
        val referralStatus = if (isSevereOrActNow) {
            if (isHindi) "तत्काल कृषि विशेषज्ञ सहायता आवश्यक" else "Immediate Referral Required"
        } else {
            if (isHindi) "नियमित निगरानी रखें" else "Routine Field Monitoring"
        }
        val referralContact = if (isHindi) "केवीके या किसान कॉल सेंटर 1800-180-1551" else "KVK or Kisan Call Centre 1800-180-1551"

        return if (isHindi) {
            """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Agri Care AI assistant in simple Hindi (सरल हिंदी में).
- Explain in plain words for a smallholder farmer in at most 6 short, complete sentences.
- Answer completely in Hindi (संपूर्ण उत्तर केवल हिंदी में दें).
- Use only steps in the entry below.
- Do not give doses (follow product label).
- Do not name any product not in the entry.
- Do not change severity or referral advice.
1. क्या लक्षण हैं:
2. अभी क्या करें:
3. कृषि अधिकारी से संपर्क कब करें:<|eot_id|><|start_header_id|>user<|end_header_id|>
Finding: $crop, $diseaseName (${kbEntry.type}), $severity ($severityDamageDescription), $certainty, $urgency.
- Action: $urgentStep
- Natural: $naturalRemedy
- Referral: $referralStatus ($referralContact)
Explain practical steps for the farmer in Hindi (किसान के लिए व्यावहारिक कदम हिंदी में समझाएं).<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""
        } else {
            """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Agri Care AI assistant$langInstruction.
- Explain in plain words for a smallholder farmer in at most 6 short, complete sentences.
- Use only steps in the entry below.
- Do not give doses (follow product label).
- Do not name any product not in the entry.
- Do not change severity or referral advice.
1. What I see:
2. What to do now:
3. When to get help:<|eot_id|><|start_header_id|>user<|end_header_id|>
Finding: $crop, $diseaseName (${kbEntry.type}), $severity ($severityDamageDescription), $certainty, $urgency.
- Action: $urgentStep
- Natural: $naturalRemedy
- Referral: $referralStatus ($referralContact)
Explain practical steps for the farmer.<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""
        }
    }
}
