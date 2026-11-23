package com.cropdoctor.chat

/**
 * Maps farmer queries across English, Hindi, and Telugu to the 8 standard question types.
 */
object QuestionKeywordMatcher {

    data class KeywordPattern(
        val section: String,
        val englishKeywords: List<String>,
        val hindiKeywords: List<String>,
        val teluguKeywords: List<String>
    )

    private val PATTERNS = listOf(
        KeywordPattern(
            section = QuestionSection.SYMPTOMS,
            englishKeywords = listOf("symptoms", "symptom", "signs", "damage", "spots", "lesions", "pustules", "early warning", "look like", "how to recognize", "blight spots"),
            hindiKeywords = listOf("लक्षण", "संकेत", "धब्बे", "नुकसान", "पहचान कैसे", "दिखता कैसा", "फफूंद", "पत्ती धब्बे"),
            teluguKeywords = listOf("లక్షణాలు", "లక్షణం", "గుర్తులు", "మచ్చలు", "నష్టం", "ఎలా ఉంటుంది", "గుర్తించడం ఎలా", "తెగులు గుర్తులు")
        ),
        KeywordPattern(
            section = QuestionSection.WHAT_TO_DO_NOW,
            englishKeywords = listOf("what to do now", "immediate", "urgent", "action", "first step", "right now", "emergency", "what should i do", "how to treat now"),
            hindiKeywords = listOf("अभी क्या करें", "तुरंत क्या करें", "तत्काल कदम", "पहला कदम", "उपाय", "अभी", "तुरंत", "क्या उपाय करें"),
            teluguKeywords = listOf("వెంటనే ఏమి చేయాలి", "ఇప్పుడు ఏమి చేయాలి", "తక్షణ చర్య", "వెంటనే", "ప్రథమ చికిత్స", "ఇప్పుడేం చేయాలి", "వెంటనే ఏం చేయాలి")
        ),
        KeywordPattern(
            section = QuestionSection.PREVENTION,
            englishKeywords = listOf("prevention", "prevent", "field practice", "crop rotation", "spacing", "avoid", "management", "future", "next season", "sanitation"),
            hindiKeywords = listOf("रोकथाम", "बचाव", "फसल चक्र", "प्रबंधन", "खेत की तैयारी", "भविष्य", "अगली फसल", "सावधानी"),
            teluguKeywords = listOf("నివారణ", "నివారణ పద్ధతులు", "యాజమాన్యం", "పంట మార్పిడి", "ముందు జాగ్రత్తలు", "భవిష్యత్తు", "మళ్లీ రాకుండా")
        ),
        KeywordPattern(
            section = QuestionSection.NATURAL_REMEDY,
            englishKeywords = listOf("natural remedy", "natural", "organic", "traditional", "bio", "neem", "trichoderma", "ash", "cow urine", "botanical", "home remedy"),
            hindiKeywords = listOf("प्राकृतिक उपचार", "जैविक", "नीम", "देसी नुस्खे", "ट्राइकोडर्मा", "राख", "गोमूत्र", "घरेलू उपचार"),
            teluguKeywords = listOf("సేంద్రీయ నివారణ", "సేంద్రీయ", "వేప", "సహజ", "సాంప్రదాయ", "జీవ నియంత్రణ", "కషాయం", "బూడిద", "దేశీ చిట్కాలు")
        ),
        KeywordPattern(
            section = QuestionSection.CHEMICAL,
            englishKeywords = listOf("chemical", "fungicide", "insecticide", "spray", "medicine", "active ingredient", "cibrc", "pesticide", "recommended spray"),
            hindiKeywords = listOf("रासायनिक दवा", "दवा", "छिड़काव", "कीटनाशक", "फफूंदनाशक", "स्प्रे", "केमिकल", "दवा का नाम"),
            teluguKeywords = listOf("రసాయన మందులు", "మందు", "రసాయన", "పిచికారీ", "స్ప్రే", "పురుగుమందు", "శిలీంధ్రనాశిని", "రసాయనాలు", "మందుల వివరాలు")
        ),
        KeywordPattern(
            section = QuestionSection.SAFETY_WAITING_PERIOD,
            englishKeywords = listOf("safety", "waiting period", "phi", "pre-harvest", "gloves", "mask", "interval", "days before harvest", "precautions", "safe to eat"),
            hindiKeywords = listOf("सुरक्षा", "प्रतीक्षा अवधि", "कटाई से पहले", "दस्ताने", "मास्क", "कितने दिन", "कटाई अवधि", "सावधानियां"),
            teluguKeywords = listOf("రక్షణ", "వేచి ఉండే సమయం", "కోతకు ముందు", "వ్యవధి", "గ్లౌజులు", "మాస్క్", "రోజులు", "జాగ్రత్తలు", "సురక్షిత వ్యవధి")
        ),
        KeywordPattern(
            section = QuestionSection.WHEN_TO_CALL_OFFICER,
            englishKeywords = listOf("when to call the officer", "call", "officer", "extension", "helpline", "expert", "kisan call centre", "1800-180-1551", "kvk", "phone"),
            hindiKeywords = listOf("अधिकारी को कब बुलाएं", "कृषि अधिकारी", "संपर्क", "फोन", "हेल्पलाइन", "किसान कॉल सेंटर", "1800-180-1551", "विशेषज्ञ"),
            teluguKeywords = listOf("అధికారిని ఎప్పుడు కలవాలి", "వ్యవసాయ అధికారి", "ఫోన్", "కాల్", "హెల్ప్‌లైన్", "కిసాన్ కాల్ సెంటర్", "1800-180-1551", "సంప్రదించండి", "అధికారి")
        ),
        KeywordPattern(
            section = QuestionSection.WHAT_IS_IT,
            englishKeywords = listOf("what is it", "what is this", "what disease", "what pest", "about this", "identify", "cause", "pathogen", "disease name", "overview", "tell me about this"),
            hindiKeywords = listOf("यह क्या है", "यह कौन सा रोग है", "रोग का नाम", "कारण", "पहचान", "इसके बारे में बताएं"),
            teluguKeywords = listOf("ఇది ఏమిటి", "ఈ తెగులు ఏమిటి", "తెగులు పేరు", "కారణం", "వివరణ", "పరిచయం", "ఏమిటి?", "ఏమిటి")
        )
    )

    /**
     * Identifies the primary section matching the user's question keywords.
     * Returns null if no recognized keywords match.
     * Selects the pattern with the longest matching keyword to prevent short keywords
     * from shadowing specific compound phrases.
     */
    fun matchSection(query: String, language: String): String? {
        val q = query.lowercase().trim()
        if (q.isBlank()) return null

        var bestSection: String? = null
        var maxMatchLength = 0

        for (p in PATTERNS) {
            val keywords = when (language.lowercase()) {
                "te" -> p.teluguKeywords + p.englishKeywords
                "hi" -> p.hindiKeywords + p.englishKeywords
                else -> p.englishKeywords
            }
            for (kw in keywords) {
                if (q.contains(kw) && kw.length > maxMatchLength) {
                    maxMatchLength = kw.length
                    bestSection = p.section
                }
            }
        }
        return bestSection
    }

    /**
     * Returns the user-facing localized chip titles for the 8 question types.
     */
    fun getQuestionChips(language: String): List<Pair<String, String>> {
        return when (language.lowercase()) {
            "te" -> listOf(
                QuestionSection.WHAT_IS_IT to "ఏమిటి?",
                QuestionSection.SYMPTOMS to "లక్షణాలు",
                QuestionSection.WHAT_TO_DO_NOW to "వెంటనే ఏమి చేయాలి?",
                QuestionSection.PREVENTION to "నివారణ పద్ధతులు",
                QuestionSection.NATURAL_REMEDY to "సేంద్రీయ నివారణ",
                QuestionSection.CHEMICAL to "రసాయన మందులు",
                QuestionSection.SAFETY_WAITING_PERIOD to "రక్షణ & వేచి ఉండే సమయం",
                QuestionSection.WHEN_TO_CALL_OFFICER to "అధికారిని ఎప్పుడు కలవాలి?"
            )
            "hi" -> listOf(
                QuestionSection.WHAT_IS_IT to "यह क्या है?",
                QuestionSection.SYMPTOMS to "लक्षण",
                QuestionSection.WHAT_TO_DO_NOW to "अभी क्या करें?",
                QuestionSection.PREVENTION to "रोकथाम",
                QuestionSection.NATURAL_REMEDY to "प्राकृतिक उपचार",
                QuestionSection.CHEMICAL to "रासायनिक दवा",
                QuestionSection.SAFETY_WAITING_PERIOD to "सुरक्षा व प्रतीक्षा अवधि",
                QuestionSection.WHEN_TO_CALL_OFFICER to "अधिकारी को कब बुलाएं?"
            )
            else -> listOf(
                QuestionSection.WHAT_IS_IT to "What is it?",
                QuestionSection.SYMPTOMS to "Symptoms",
                QuestionSection.WHAT_TO_DO_NOW to "What to do now",
                QuestionSection.PREVENTION to "Prevention",
                QuestionSection.NATURAL_REMEDY to "Natural remedy",
                QuestionSection.CHEMICAL to "Chemical",
                QuestionSection.SAFETY_WAITING_PERIOD to "Safety & waiting period",
                QuestionSection.WHEN_TO_CALL_OFFICER to "When to call officer"
            )
        }
    }
}
