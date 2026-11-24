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
            englishKeywords = listOf(
                "symptoms", "symptom", "signs", "damage", "spots", "lesions", "pustules",
                "early warning", "look like", "looks like", "how to recognize", "blight spots",
                "how does it look", "what does it look like", "appearance", "how to identify",
                "color", "colour", "leaf damage", "severity", "how severe", "is it serious",
                "serious", "danger", "dangerous", "harmful"
            ),
            hindiKeywords = listOf(
                "लक्षण", "संकेत", "धब्बे", "नुकसान", "पहचान कैसे", "दिखता कैसा", "फफूंद", "पत्ती धब्बे",
                "पहचान", "कैसा दिखता है", "पत्तियां", "रंग", "गंभीरता", "क्या लक्षण हैं", "नुकसानदायक"
            ),
            teluguKeywords = listOf(
                "లక్షణాలు", "లక్షణం", "గుర్తులు", "మచ్చలు", "నష్టం", "ఎలా ఉంటుంది", "గుర్తించడం ఎలా",
                "తెగులు గుర్తులు", "ఎలా గుర్తించాలి", "రంగు", "తీవ్రత", "ప్రమాదకరమా"
            )
        ),
        KeywordPattern(
            section = QuestionSection.WHAT_TO_DO_NOW,
            englishKeywords = listOf(
                "what to do now", "what to do", "what should i do", "what can i do", "immediate",
                "urgent", "action", "first step", "right now", "emergency", "how to treat now",
                "how to treat", "treat", "treatment", "how to cure", "cure", "how to control",
                "control", "how to fix", "fix", "how to stop", "stop", "how to save", "save",
                "how to manage", "manage", "management", "solution", "solutions", "how to solve",
                "suggest", "suggestion", "suggestions", "advice", "advise", "recommend",
                "recommendation", "recommendations", "guidance", "what next", "next step",
                "steps", "help me", "what can be done", "how to handle"
            ),
            hindiKeywords = listOf(
                "अभी क्या करें", "तुरंत क्या करें", "तत्काल कदम", "पहला कदम", "उपाय", "अभी", "तुरंत",
                "क्या उपाय करें", "इलाज", "उपचार", "कैसे ठीक करें", "नियंत्रण", "रोकथाम", "क्या करें",
                "कैसे बचाएं", "बचाव", "समाधान", "क्या करना चाहिए", "सलाह", "सुझाव", "क्या कदम उठाएं"
            ),
            teluguKeywords = listOf(
                "వెంటనే ఏమి చేయాలి", "ఇప్పుడు ఏమి చేయాలి", "తక్షణ చర్య", "వెంటనే", "ప్రథమ చికిత్స",
                "ఇప్పుడేం చేయాలి", "వెంటనే ఏం చేయాలి", "చికిత్స", "ఎలా తగ్గించాలి", "ఎలా నియంత్రించాలి",
                "ఏం చేయాలి", "ఎలా కాపాడాలి", "పరిష్కారం", "సలహా", "సూచన"
            )
        ),
        KeywordPattern(
            section = QuestionSection.PREVENTION,
            englishKeywords = listOf(
                "prevention", "prevent", "field practice", "crop rotation", "spacing", "avoid",
                "management", "future", "next season", "sanitation", "protect", "how to prevent",
                "preventing", "stop recurring", "how to avoid"
            ),
            hindiKeywords = listOf(
                "रोकथाम", "बचाव", "फसल चक्र", "प्रबंधन", "खेत की तैयारी", "भविष्य", "अगली फसल",
                "सावधानी", "रोकें", "दोबारा न आए"
            ),
            teluguKeywords = listOf(
                "నివారణ", "నివారణ పద్ధతులు", "యాజమాన్యం", "పంట మార్పిడి", "ముందు జాగ్రత్తలు",
                "భవిష్యత్తు", "మళ్లీ రాకుండా", "వచ్చే పంట"
            )
        ),
        KeywordPattern(
            section = QuestionSection.NATURAL_REMEDY,
            englishKeywords = listOf(
                "natural remedy", "natural", "organic", "traditional", "bio", "neem",
                "trichoderma", "ash", "cow urine", "botanical", "home remedy", "remedy",
                "remedies", "biological", "desi", "herbal", "home remedies"
            ),
            hindiKeywords = listOf(
                "प्राकृतिक उपचार", "जैविक", "नीम", "देसी नुस्खे", "ट्राइकोडर्मा", "राख", "गोमूत्र",
                "घरेलू उपचार", "घरेलू", "देसी", "प्राकृतिक"
            ),
            teluguKeywords = listOf(
                "సేంద్రీయ నివారణ", "సేంద్రీయ", "వేప", "సహజ", "సాంప్రదాయ", "జీవ నియంత్రణ",
                "కషాయం", "బూడిద", "దేశీ చిట్కాలు", "చిట్కాలు"
            )
        ),
        KeywordPattern(
            section = QuestionSection.CHEMICAL,
            englishKeywords = listOf(
                "chemical", "fungicide", "fungicides", "insecticide", "insecticides", "spray",
                "sprays", "medicine", "medicines", "active ingredient", "cibrc", "pesticide",
                "pesticides", "recommended spray", "dose", "dosage", "which spray", "which medicine",
                "which chemical", "what to spray", "how much spray"
            ),
            hindiKeywords = listOf(
                "रासायनिक दवा", "दवा", "छिड़काव", "कीटनाशक", "फफूंदनाशक", "स्प्रे", "केमिकल",
                "दवा का नाम", "दवाएं", "मात्रा", "डोज़", "कौन सी दवा", "क्या छिड़कें"
            ),
            teluguKeywords = listOf(
                "రసాయన మందులు", "మందు", "రసాయన", "పిచికారీ", "స్ప్రే", "పురుగుమందు", "శిలీంధ్రనాశిని",
                "రసాయనాలు", "మందుల వివరాలు", "మందులు", "మోతాదు", "ఏ మందు", "ఏం పిచికారీ చేయాలి"
            )
        ),
        KeywordPattern(
            section = QuestionSection.SAFETY_WAITING_PERIOD,
            englishKeywords = listOf(
                "safety", "waiting period", "phi", "pre-harvest", "gloves", "mask", "interval",
                "days before harvest", "precautions", "safe to eat", "safe", "precaution",
                "harvest", "when to harvest", "waiting days", "protective"
            ),
            hindiKeywords = listOf(
                "सुरक्षा", "प्रतीक्षा अवधि", "कटाई से पहले", "दस्ताने", "मास्क", "कितने दिन",
                "कटाई अवधि", "सावधानियां", "सुरक्षित", "सावधानी"
            ),
            teluguKeywords = listOf(
                "రక్షణ", "వేచి ఉండే సమయం", "కోతకు ముందు", "వ్యవధి", "గ్లౌజులు", "మాస్క్",
                "రోజులు", "జాగ్రత్తలు", "సురక్షిత వ్యవధి", "జాగ్రత్త"
            )
        ),
        KeywordPattern(
            section = QuestionSection.WHEN_TO_CALL_OFFICER,
            englishKeywords = listOf(
                "when to call the officer", "call", "officer", "extension", "helpline", "expert",
                "kisan call centre", "1800-180-1551", "kvk", "phone", "contact", "help",
                "number", "toll free", "call officer", "call the officer"
            ),
            hindiKeywords = listOf(
                "अधिकारी को कब बुलाएं", "कृषि अधिकारी", "संपर्क", "फोन", "हेल्पलाइन",
                "किसान कॉल सेंटर", "1800-180-1551", "विशेषज्ञ", "नंबर", "मदद"
            ),
            teluguKeywords = listOf(
                "అధికారిని ఎప్పుడు కలవాలి", "వ్యవసాయ అధికారి", "ఫోన్", "కాల్", "హెల్ప్‌లైన్",
                "కిసాన్ కాల్ సెంటర్", "1800-180-1551", "సంప్రదించండి", "అధికారి", "నెంబర్"
            )
        ),
        KeywordPattern(
            section = QuestionSection.WHAT_IS_IT,
            englishKeywords = listOf(
                "what is it", "what is this", "what is that", "what disease", "what pest", "about this",
                "identify", "identification", "cause", "causes", "caused", "causing", "pathogen", "disease name",
                "overview", "tell me about this", "tell me about", "tell me more", "explain", "explanation",
                "details", "condition", "what condition", "problem", "problems", "what is the problem",
                "what problem", "what exactly problem here", "issue", "issues", "wrong", "what is wrong",
                "what is the issue", "why it occurred", "why did this occur", "why did it happen",
                "why is this happening", "why", "occur", "occurred", "occurrence", "happen", "happened",
                "reason", "reasons", "origin", "where did it come from", "how did it happen", "how does it spread",
                "spread", "spreads", "spreading", "fungus", "fungal", "bacteria", "bacterial", "virus", "viral",
                "pest", "disease", "infection", "infected", "information", "info"
            ),
            hindiKeywords = listOf(
                "यह क्या है", "यह कौन सा रोग है", "रोग का नाम", "कारण", "पहचान", "इसके बारे में बताएं",
                "क्यों हुआ", "क्यों आया", "वजह", "उत्पत्ति", "कैसे फैलता है", "फैलता", "फैलना",
                "बीमारी", "यह कैसे हुआ", "कारण क्या है", "समस्या", "दिक्कत", "परेशानी", "क्या हुआ",
                "क्या खराबी है", "जानकारी", "विवरण", "बताओ", "समझाइए"
            ),
            teluguKeywords = listOf(
                "ఇది ఏమిటి", "ఈ తెగులు ఏమిటి", "తెగులు పేరు", "కారణం", "వివరణ", "పరిచయం",
                "ఏమిటి?", "ఏమిటి", "ఎందుకు వచ్చింది", "ఎలా వ్యాపిస్తుంది", "ఎందుకు",
                "ఎలా వచ్చింది", "కారణాలు", "సమస్య", "ఏం జరిగింది", "పంటకు ఏమైంది",
                "వివరాలు", "తెలియజేయండి"
            )
        )
    )

    /**
     * Identifies the primary section matching the user's question keywords.
     * Returns null if no recognized keywords match.
     * Selects the pattern with the longest matching keyword to prevent short keywords
     * from shadowing specific compound phrases.
     */
    fun matchSection(query: String, language: String): String? {
        val q = query.lowercase().trim().replace(Regex("\\s+"), " ")
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
