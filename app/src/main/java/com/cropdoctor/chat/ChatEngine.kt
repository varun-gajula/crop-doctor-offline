package com.cropdoctor.chat

/**
 * Result data class for a chat response.
 */
data class ChatResponse(
    val text: String,
    val label: String, // "Explained on this phone by Llama 3.2 1B" or "Offline guide"
    val source: String,
    val retrievedPassages: List<Passage>,
    val isNoMatch: Boolean = false,
    val advisorSource: String = "OFFLINE_GUIDE", // "LLAMA" or "OFFLINE_GUIDE"
    val guardrailPassed: Boolean = true,
    val guardrailReason: String? = null
)

/**
 * Offline Chat Engine for Crop Doctor (Task CHAT).
 *
 * Enforces:
 * 1. Offline retrieval via SQLite FTS5 for (detected class, question keywords, language).
 * 2. English & Hindi answered by Llama 3.2 1B ONLY from retrieved passages, validated by ChatGuardrail.
 * 3. Telugu: Llama does NOT write Telugu. Shows reviewed Telugu passage text.
 * 4. No-passage fallback: "I do not have a sourced answer for this. Please call the extension officer (1800-180-1551)."
 * 5. Source line under every answer.
 */
object ChatEngine {

    const val LABEL_LLAMA = "Explained on this phone by Llama 3.2 1B"
    const val LABEL_OFFLINE_GUIDE_EN = "Offline guide"
    const val LABEL_OFFLINE_GUIDE_TE = "ఆఫ్‌లైన్ గైడ్"
    const val LABEL_OFFLINE_GUIDE_HI = "ऑफलाइन गाइड"

    const val NO_MATCH_ANSWER_EN =
        "I do not have a sourced answer for this. Please call the extension officer (1800-180-1551)."
    const val NO_MATCH_ANSWER_TE =
        "దీనికి నా వద్ద అధికారిక సమాచారం లేదు. దయచేసి వ్యవసాయ విస్తరణ అధికారిని (1800-180-1551) సంప్రదించండి."
    const val NO_MATCH_ANSWER_HI =
        "मेरे पास इसके लिए कोई प्रमाणित उत्तर नहीं है। कृपया कृषि विस्तार अधिकारी (1800-180-1551) से संपर्क करें।"

    fun getOfflineGuideLabel(language: String): String {
        return when (language.lowercase()) {
            "te" -> LABEL_OFFLINE_GUIDE_TE
            "hi" -> LABEL_OFFLINE_GUIDE_HI
            else -> LABEL_OFFLINE_GUIDE_EN
        }
    }

    fun getNoMatchText(language: String): String {
        return when (language.lowercase()) {
            "te" -> NO_MATCH_ANSWER_TE
            "hi" -> NO_MATCH_ANSWER_HI
            else -> NO_MATCH_ANSWER_EN
        }
    }

    fun buildPrompt(
        crop: String,
        problemName: String,
        severity: String,
        certainty: String,
        passages: List<Passage>,
        query: String
    ): String {
        val topPassage = passages.firstOrNull()?.text?.take(450) ?: ""
        return """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Crop Doctor offline chat assistant.
- Answer in 1 or 2 short sentences (at most 35 words).
- End with a complete sentence.
- Use ONLY the Passage below. Do NOT add new chemicals, doses, or numbers.
- If the Passage does not answer the question, state: "$NO_MATCH_ANSWER_EN"<|eot_id|><|start_header_id|>user<|end_header_id|>
Finding: $crop, $problemName, $severity ($certainty)
Passage: $topPassage
Question: $query<|eot_id|><|start_header_id|>assistant<|end_header_id|>""".trimIndent()
    }

    suspend fun processQuery(
        query: String,
        detectedClass: String,
        crop: String,
        problemName: String,
        severity: String,
        certainty: String,
        language: String,
        passageStore: PassageStore,
        isUncertainOrOutOfClass: Boolean = false,
        llamaGenerator: (suspend (prompt: String) -> String)? = null
    ): ChatResponse {
        val q = query.trim()
        val lang = language.lowercase()

        // Requirement 5: Chat is disabled for Can't tell and Out of class outcomes
        if (isUncertainOrOutOfClass) {
            val disabledMsg = when (lang) {
                "te" -> "సమస్య నిర్ధారణ కానప్పుడు లేదా పంట కానప్పుడు చాట్ అందుబాటులో ఉండదు. దయచేసి వ్యవసాయ అధికారిని సంప్రదించండి."
                "hi" -> "स्थिति अनिश्चित होने या गैर-फसल होने पर चैट उपलब्ध नहीं है। कृपया कृषि अधिकारी से संपर्क करें।"
                else -> "Chat is unavailable when condition is uncertain or non-crop. Please consult an extension officer."
            }
            return ChatResponse(
                text = disabledMsg,
                label = getOfflineGuideLabel(lang),
                source = "Kisan Call Centre (1800-180-1551)",
                retrievedPassages = emptyList(),
                isNoMatch = true,
                advisorSource = "OFFLINE_GUIDE"
            )
        }

        // 1. Retrieve best 1-3 passages from SQLite FTS5
        val passages = passageStore.getBestPassages(
            detectedClass = detectedClass,
            questionKeywords = q,
            language = lang,
            limit = 3
        )

        // 4. If no passage matches: return standard no-match message
        if (passages.isEmpty()) {
            return ChatResponse(
                text = getNoMatchText(lang),
                label = getOfflineGuideLabel(lang),
                source = "National Kisan Call Centre (Toll-Free 1800-180-1551)",
                retrievedPassages = emptyList(),
                isNoMatch = true,
                advisorSource = "OFFLINE_GUIDE"
            )
        }

        val primarySource = passages.firstOrNull()?.source ?: "ICAR / SAU Official Package of Practices"
        val fallbackPassageText = passages.joinToString("\n\n") { it.text }

        // 3. Telugu: Llama does NOT write Telugu. Show reviewed Telugu passage text.
        if (lang == "te") {
            return ChatResponse(
                text = fallbackPassageText,
                label = getOfflineGuideLabel("te"),
                source = primarySource,
                retrievedPassages = passages,
                isNoMatch = false,
                advisorSource = "OFFLINE_GUIDE"
            )
        }

        // 2. English & Hindi: Try generating with Llama if available
        if (llamaGenerator != null) {
            val prompt = buildPrompt(
                crop = crop,
                problemName = problemName,
                severity = severity,
                certainty = certainty,
                passages = passages,
                query = q
            )

            try {
                val rawAnswer = llamaGenerator(prompt).trim()
                val (cleanAnswer, wasCut) = com.cropdoctor.advisor.SentenceTrimmer.cleanToCompleteSentence(rawAnswer, lang)
                val candidateAnswer = if (cleanAnswer.isNotBlank()) cleanAnswer else rawAnswer
                val findingText = "Crop: $crop Problem: $problemName Severity: $severity Certainty: $certainty"

                // Validate with Guardrail
                val validation = ChatGuardrail.validate(
                    generatedAnswer = candidateAnswer,
                    passages = passages,
                    findingText = findingText
                )

                if (validation.isValid) {
                    return ChatResponse(
                        text = candidateAnswer,
                        label = LABEL_LLAMA,
                        source = primarySource,
                        retrievedPassages = passages,
                        isNoMatch = false,
                        advisorSource = "LLAMA",
                        guardrailPassed = true
                    )
                } else {
                    // On rejection, show passage text with label "Offline guide"
                    return ChatResponse(
                        text = fallbackPassageText,
                        label = getOfflineGuideLabel(lang),
                        source = primarySource,
                        retrievedPassages = passages,
                        isNoMatch = false,
                        advisorSource = "OFFLINE_GUIDE",
                        guardrailPassed = false,
                        guardrailReason = validation.failureReason
                    )
                }
            } catch (e: Exception) {
                // On error fallback to passage text
            }
        }

        // Default / Fallback to offline guide text
        return ChatResponse(
            text = fallbackPassageText,
            label = getOfflineGuideLabel(lang),
            source = primarySource,
            retrievedPassages = passages,
            isNoMatch = false,
            advisorSource = "OFFLINE_GUIDE"
        )
    }
}
