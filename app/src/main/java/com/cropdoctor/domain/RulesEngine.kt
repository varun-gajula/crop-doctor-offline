package com.cropdoctor.domain

import com.cropdoctor.R
import com.cropdoctor.advisor.GuardrailFilter
import com.cropdoctor.knowledge.KbEntry

object RulesEngine {

    const val EXTENSION_OFFICER_NUMBER = "18001801551"
    const val EXTENSION_OFFICER_LABEL = "Kisan Call Centre"

    enum class OpenTier {
        NATURAL_FIRST,
        BOTH_SHOWN,
        CHEMICAL_FIRST
    }

    enum class FieldSpread {
        FEW_PLANTS,
        SOME,
        MOST
    }

    enum class UrgencyLevel {
        EARLY_WARNING,
        ACT_THIS_WEEK,
        ACT_NOW
    }

    /**
     * Maps raw classifier confidence to farmer-readable certainty bands:
     * - Sure: >= 0.80
     * - Probably: 0.60 to 0.799
     * - Not sure: < 0.60
     */
    fun calculateCertainty(confidence: Float): CertaintyBand {
        return when {
            confidence >= 0.80f -> CertaintyBand.SURE
            confidence >= 0.60f -> CertaintyBand.PROBABLY
            else -> CertaintyBand.NOT_SURE
        }
    }

    /**
     * Calculates severity band from lesion area fraction (< 10% mild, 10-30% moderate, > 30% severe)
     * and specific pathology rules via SeverityCalculator.
     */
    fun calculateSeverity(lesionFraction: Float, classId: String = ""): SeverityBand {
        return SeverityCalculator.calculate(lesionFraction, classId)
    }

    /**
     * Requirement F4 / Task G3:
     * Combines leaf-damage severity band with the farmer's field spread answer into an urgency level.
     * Full decision table documented in docs/RULES.md.
     */
    fun calculateUrgency(severityBand: SeverityBand, fieldSpread: FieldSpread): UrgencyLevel {
        return when (severityBand) {
            SeverityBand.MILD -> when (fieldSpread) {
                FieldSpread.FEW_PLANTS -> UrgencyLevel.EARLY_WARNING
                FieldSpread.SOME -> UrgencyLevel.ACT_THIS_WEEK
                FieldSpread.MOST -> UrgencyLevel.ACT_NOW
            }
            SeverityBand.MODERATE -> when (fieldSpread) {
                FieldSpread.FEW_PLANTS -> UrgencyLevel.ACT_THIS_WEEK
                FieldSpread.SOME, FieldSpread.MOST -> UrgencyLevel.ACT_NOW
            }
            SeverityBand.SEVERE -> UrgencyLevel.ACT_NOW
        }
    }

    /**
     * openTier decision logic based on UrgencyLevel:
     * - Early Warning: Natural tier opened first
     * - Act This Week: Both shown
     * - Act Now: Chemical tier + extension worker first
     */
    fun determineOpenTier(urgencyLevel: UrgencyLevel): OpenTier {
        return when (urgencyLevel) {
            UrgencyLevel.EARLY_WARNING -> OpenTier.NATURAL_FIRST
            UrgencyLevel.ACT_THIS_WEEK -> OpenTier.BOTH_SHOWN
            UrgencyLevel.ACT_NOW -> OpenTier.CHEMICAL_FIRST
        }
    }

    /**
     * openTier decision logic (fallback by severity alone).
     */
    fun determineOpenTier(severityBand: SeverityBand): OpenTier {
        return when (severityBand) {
            SeverityBand.MILD -> OpenTier.NATURAL_FIRST
            SeverityBand.MODERATE -> OpenTier.BOTH_SHOWN
            SeverityBand.SEVERE -> OpenTier.CHEMICAL_FIRST
        }
    }

    /**
     * Determines whether the field spread question should be shown.
     * Question is shown for "Sure" and "Probably", and skipped for "Not sure" or healthy plants.
     */
    fun shouldShowFieldSpreadQuestion(certaintyBand: CertaintyBand, isHealthy: Boolean = false): Boolean {
        return certaintyBand != CertaintyBand.NOT_SURE && !isHealthy
    }

    /**
     * Referral rule: Not sure, severe, or Act Now urgency -> red "Talk to an extension officer" button.
     */
    fun shouldShowExtensionWorker(certaintyBand: CertaintyBand, urgencyLevel: UrgencyLevel): Boolean {
        return certaintyBand == CertaintyBand.NOT_SURE || urgencyLevel == UrgencyLevel.ACT_NOW
    }

    fun shouldShowExtensionWorker(certaintyBand: CertaintyBand, severityBand: SeverityBand): Boolean {
        return certaintyBand == CertaintyBand.NOT_SURE || severityBand == SeverityBand.SEVERE
    }

    /**
     * T2b: Determines whether to show severity band to the farmer.
     * When certainty is NOT_SURE, severity must not be shown.
     */
    fun shouldShowSeverity(certaintyBand: CertaintyBand): Boolean {
        return certaintyBand != CertaintyBand.NOT_SURE
    }

    /**
     * T2b: Determines whether to show the early warning action sentence.
     * When certainty is NOT_SURE, early warning must not be shown.
     */
    fun shouldShowEarlyWarning(certaintyBand: CertaintyBand, severityBand: SeverityBand): Boolean {
        return certaintyBand != CertaintyBand.NOT_SURE && severityBand == SeverityBand.MILD
    }

    /**
     * T2b: Resolves guidance / status sentence:
     * - If certainty is NOT_SURE -> R.string.cant_tell_no_disease ("Can’t tell – no disease identified")
     * - If MILD -> R.string.severity_early_warning (or severity_pest_early_warning for pests)
     * - Else -> R.string.severity_act_now (or severity_pest_act_now for pests)
     */
    fun getGuidanceSentence(certaintyBand: CertaintyBand, severityBand: SeverityBand, type: String = "disease"): Int {
        if (certaintyBand == CertaintyBand.NOT_SURE) {
            return R.string.cant_tell_no_disease
        }
        return getSeverityActionSentence(severityBand, type)
    }

    /**
     * Severity action sentence:
     * - Mild -> early warning
     * - Moderate/Severe -> act now
     * Pest results use pest-specific monitoring & ETL guidance (Rule G1).
     */
    fun getSeverityActionSentence(severityBand: SeverityBand, type: String = "disease"): Int {
        if (type.equals("pest", ignoreCase = true)) {
            return when (severityBand) {
                SeverityBand.MILD -> R.string.severity_pest_early_warning
                SeverityBand.MODERATE, SeverityBand.SEVERE -> R.string.severity_pest_act_now
            }
        }
        return when (severityBand) {
            SeverityBand.MILD -> R.string.severity_early_warning
            SeverityBand.MODERATE, SeverityBand.SEVERE -> R.string.severity_act_now
        }
    }

    /**
     * Determines whether the app should prompt the farmer to call an agricultural extension worker (KVK/DAE).
     */
    fun shouldReferToKVK(severityBand: SeverityBand, referralThreshold: String): Boolean {
        return when (referralThreshold.uppercase()) {
            "NONE" -> false
            "MODERATE" -> severityBand == SeverityBand.MODERATE || severityBand == SeverityBand.SEVERE
            "SEVERE" -> severityBand == SeverityBand.SEVERE
            else -> severityBand == SeverityBand.SEVERE
        }
    }

    /**
     * Healthy plants never receive chemical fungicide/pesticide recommendations.
     */
    fun isChemicalTreatmentApplicable(classId: String): Boolean {
        return !classId.endsWith("_healthy")
    }

    /**
     * Requirement F4 / Task A2:
     * Official referral sentence checked and added by code (never invented or changed by Llama).
     */
    fun getReferralSentence(
        urgencyLevel: UrgencyLevel,
        certaintyBand: CertaintyBand,
        language: String
    ): String {
        val requiresReferral = shouldShowExtensionWorker(certaintyBand, urgencyLevel)
        return when (language.lowercase()) {
            "te" -> {
                if (requiresReferral) {
                    "వెంటనే మీ స్థానిక వ్యవసాయ విస్తరణ అధికారిని (AEO) సంప్రదించండి లేదా కిసాన్ కాల్ సెంటర్ 1800-180-1551 కు కాల్ చేయండి."
                } else {
                    "పొలాన్ని క్రమం తప్పకుండా పర్యవేక్షించండి. రాబోయే 3-5 రోజుల్లో వ్యాప్తి పెరిగితే వ్యవసాయ అధికారిని లేదా 1800-180-1551 ని సంప్రదించండి."
                }
            }
            "hi" -> {
                if (requiresReferral) {
                    "तत्काल अपने स्थानीय कृषि विस्तार अधिकारी से संपर्क करें या किसान कॉल सेंटर 1800-180-1551 पर कॉल करें।"
                } else {
                    "खेत की नियमित निगरानी करें। यदि अगले 3-5 दिनों में लक्षण बढ़ते हैं, तो कृषि विस्तार अधिकारी या 1800-180-1551 पर संपर्क करें।"
                }
            }
            else -> {
                if (requiresReferral) {
                    "Contact your local agricultural extension officer immediately or call Kisan Call Centre at 1800-180-1551."
                } else {
                    "Monitor the field regularly. If symptoms spread to more plants over the next 3-5 days, contact your local extension officer or call 1800-180-1551."
                }
            }
        }
    }

    /**
     * Task A2: Builds structured case-specific advisory in three distinct parts:
     * 1. What I see
     * 2. What to do now
     * 3. When to get help
     *
     * Rule H6: For Telugu, builds entirely from verified KB text pieces and glossary terms in code.
     */
    fun buildCaseSpecificAdvisory(
        classId: String,
        crop: String,
        diseaseName: String,
        type: String,
        severityBand: SeverityBand,
        certaintyBand: CertaintyBand,
        fieldSpread: FieldSpread,
        urgencyLevel: UrgencyLevel,
        region: String,
        language: String,
        kbEntry: KbEntry
    ): String {
        val isNotSure = certaintyBand == CertaintyBand.NOT_SURE || classId.equals("unknown", ignoreCase = true)
        val referralSentence = getReferralSentence(urgencyLevel, certaintyBand, language)

        if (language.lowercase() == "te") {
            val cropTe = when (crop.lowercase()) {
                "rice" -> "వరి"
                "cotton" -> "పత్తి"
                "chilli" -> "మిరప"
                "tomato" -> "టమాటా"
                "maize" -> "మొక్కజొన్న"
                else -> crop
            }
            val typeTe = when (type.lowercase()) {
                "pest" -> "పురుగు / కీటకం"
                "healthy" -> "ఆరోగ్యకరమైన పంట"
                else -> "తెగులు"
            }
            val severityTe = when (severityBand) {
                SeverityBand.MILD -> "ప్రారంభ దశ (తక్కువ తీవ్రత)"
                SeverityBand.MODERATE -> "మధ్యస్థ తీవ్రత"
                SeverityBand.SEVERE -> "తీవ్రమైన దశ"
            }
            val spreadTe = when (fieldSpread) {
                FieldSpread.FEW_PLANTS -> "కొన్ని మొక్కలలో మాత్రమే"
                FieldSpread.SOME -> "పొలంలో కొంత భాగంలో"
                FieldSpread.MOST -> "పొలంలో ఎక్కువ భాగంలో"
            }
            val urgencyTe = when (urgencyLevel) {
                UrgencyLevel.EARLY_WARNING -> "ప్రారంభ హెచ్చరిక"
                UrgencyLevel.ACT_THIS_WEEK -> "ఈ వారంలో చర్యలు చేపట్టాలి"
                UrgencyLevel.ACT_NOW -> "తక్షణ చర్య అవసరం"
            }

            if (isNotSure) {
                return """
1. నేను గమనించినది:
పంట నమూనాలో వ్యాధి లేదా పురుగు లక్షణాలు స్పష్టంగా నిర్ధారించబడలేదు. ${kbEntry.explainText.te}

2. ఇప్పుడు చేయవలసిన పనులు:
• తక్షణ చర్య: స్పష్టమైన లక్షణాలు కనిపించే వరకు ఎటువంటి రసాయన మందులు వాడవద్దు.
• పొలంలో వివిధ ప్రదేశాల నుండి తాజా ఆకుల ఫోటోలను మంచి వెలుతురులో మళ్లీ తీయండి.

3. ఎప్పుడు సహాయం పొందాలి:
$referralSentence
                """.trimIndent()
            }

            val explainTe = kbEntry.explainText.te
            val immediateTe = kbEntry.immediateStep.te
            val sevAdviceTe = kbEntry.severityAdvice[severityBand.name]?.te ?: ""

            val stepsBuilder = StringBuilder()
            when (urgencyLevel) {
                UrgencyLevel.EARLY_WARNING -> {
                    if (kbEntry.treatment.natural.isNotEmpty()) {
                        stepsBuilder.append("• సేంద్రీయ / క్షేత్ర పద్ధతులు:\n")
                        kbEntry.treatment.natural.take(2).forEach {
                            stepsBuilder.append("  - ${it.action.te}\n")
                        }
                    }
                    stepsBuilder.append("• రాబోయే రోజుల్లో ఆకులపై కొత్త మచ్చలు లేదా తెగులు లక్షణాలు పెరుగుతున్నాయో లేదో ప్రతిరోజూ గమనించండి.")
                }
                UrgencyLevel.ACT_THIS_WEEK -> {
                    if (sevAdviceTe.isNotBlank()) {
                        stepsBuilder.append("• సిఫార్సు చేసిన నివారణ చర్య: $sevAdviceTe\n")
                    }
                    if (kbEntry.treatment.natural.isNotEmpty()) {
                        stepsBuilder.append("• క్షేత్ర నిర్వహణ: ${kbEntry.treatment.natural.first().action.te}\n")
                    }
                    if (kbEntry.treatment.chemical.isNotEmpty()) {
                        val chem = kbEntry.treatment.chemical.first()
                        stepsBuilder.append("• అవసరమైతే CIB&RC అనుమతించిన మందు: ${chem.activeIngredient} (${chem.productExample}) [లేబుల్ ప్రకారం వాడండి].")
                    }
                }
                UrgencyLevel.ACT_NOW -> {
                    if (sevAdviceTe.isNotBlank()) {
                        stepsBuilder.append("• తక్షణ అత్యవసర చర్య: $sevAdviceTe\n")
                    }
                    if (kbEntry.treatment.chemical.isNotEmpty()) {
                        stepsBuilder.append("• రసాయన నియంత్రణ: CIB&RC నమోదిత ")
                        val chems = kbEntry.treatment.chemical.joinToString(", ") { "${it.activeIngredient} (${it.productExample})" }
                        stepsBuilder.append("$chems పిచికారీ చేయండి. రక్షణ దుస్తులు ధరించండి మరియు లేబుల్ సూచనలు పాటించండి.\n")
                    }
                    stepsBuilder.append("• పొలంలో నిలిచిన నీటిని తొలగించండి మరియు తెగులు సోకిన మొక్కల భాగాలను నాశనం చేయండి.")
                }
            }

            return """
1. నేను గమనించినది:
మీ ${cropTe} పంటలో ${kbEntry.names.te} (${typeTe}) ${severityTe}లో ఉంది. ఇది ప్రస్తుతం ${spreadTe} కనిపిస్తోంది. ${explainTe}

2. ఇప్పుడు చేయవలసిన పనులు:
• తక్షణ చర్య: ${immediateTe}
• పరిస్థితి తీవ్రత (${urgencyTe}):
$stepsBuilder

3. ఎప్పుడు సహాయం పొందాలి:
$referralSentence
            """.trimIndent()
        }

        if (language.lowercase() == "hi") {
            val cropHi = when (crop.lowercase()) {
                "rice" -> "धान"
                "cotton" -> "कपास"
                "chilli" -> "मिर्च"
                "tomato" -> "टमाटर"
                "maize" -> "मक्का"
                else -> crop
            }
            val typeHi = when (type.lowercase()) {
                "pest" -> "कीट"
                "healthy" -> "स्वस्थ फसल"
                else -> "रोग"
            }
            val severityHi = when (severityBand) {
                SeverityBand.MILD -> "हल्की गंभीरता (प्रारंभिक अवस्था)"
                SeverityBand.MODERATE -> "मध्यम गंभीरता"
                SeverityBand.SEVERE -> "गंभीर अवस्था"
            }
            val spreadHi = when (fieldSpread) {
                FieldSpread.FEW_PLANTS -> "केवल कुछ पौधों में"
                FieldSpread.SOME -> "खेत के कुछ हिस्से में"
                FieldSpread.MOST -> "अधिकांश खेत में"
            }
            val urgencyHi = when (urgencyLevel) {
                UrgencyLevel.EARLY_WARNING -> "प्रारंभिक चेतावनी"
                UrgencyLevel.ACT_THIS_WEEK -> "इस सप्ताह कार्रवाई करें"
                UrgencyLevel.ACT_NOW -> "तुरंत कार्रवाई करें"
            }

            if (isNotSure) {
                return """
1. मैंने क्या देखा:
फसल के नमूने में रोग या कीट के लक्षण स्पष्ट नहीं हैं। ${kbEntry.explainText.hi}

2. अभी क्या करें:
• तत्काल कदम: स्पष्ट पहचान होने तक कोई रासायनिक कीटनाशक न डालें।
• खेत के अलग-अलग हिस्सों से अच्छी रोशनी में पत्तियों की नई तस्वीरें लें।

3. सहायता कब लें:
$referralSentence
                """.trimIndent()
            }

            val explainHi = kbEntry.explainText.hi
            val immediateHi = kbEntry.immediateStep.hi
            val sevAdviceHi = kbEntry.severityAdvice[severityBand.name]?.hi ?: ""

            val stepsBuilder = StringBuilder()
            when (urgencyLevel) {
                UrgencyLevel.EARLY_WARNING -> {
                    if (kbEntry.treatment.natural.isNotEmpty()) {
                        stepsBuilder.append("• प्राकृतिक व जैविक उपाय:\n")
                        kbEntry.treatment.natural.take(2).forEach {
                            stepsBuilder.append("  - ${it.action.hi}\n")
                        }
                    }
                    stepsBuilder.append("• आने वाले दिनों में: नियमित रूप से देखें कि क्या लक्षण अन्य पौधों में फैल रहे हैं।")
                }
                UrgencyLevel.ACT_THIS_WEEK -> {
                    if (sevAdviceHi.isNotBlank()) {
                        stepsBuilder.append("• अनुशंसित सलाह: $sevAdviceHi\n")
                    }
                    if (kbEntry.treatment.natural.isNotEmpty()) {
                        stepsBuilder.append("• खेत प्रबंधन: ${kbEntry.treatment.natural.first().action.hi}\n")
                    }
                    if (kbEntry.treatment.chemical.isNotEmpty()) {
                        val chem = kbEntry.treatment.chemical.first()
                        stepsBuilder.append("• CIB&RC अनुमोदित दवा: ${chem.activeIngredient} (${chem.productExample}) [लेबल के अनुसार]।")
                    }
                }
                UrgencyLevel.ACT_NOW -> {
                    if (sevAdviceHi.isNotBlank()) {
                        stepsBuilder.append("• तत्काल हस्तक्षेप: $sevAdviceHi\n")
                    }
                    if (kbEntry.treatment.chemical.isNotEmpty()) {
                        stepsBuilder.append("• रासायनिक नियंत्रण: CIB&RC पंजीकृत ")
                        val chems = kbEntry.treatment.chemical.joinToString(", ") { "${it.activeIngredient} (${it.productExample})" }
                        stepsBuilder.append("$chems का सुरक्षात्मक कपड़ों के साथ लेबल के अनुसार छिड़काव करें।\n")
                    }
                    stepsBuilder.append("• आने वाले दिनों में: 48 घंटे बाद खेत की जांच करें कि प्रसार रुका है या नहीं।")
                }
            }

            return """
1. मैंने क्या देखा:
आपकी $cropHi फसल में ${kbEntry.names.hi} ($typeHi) $severityHi में पाया गया है। यह $spreadHi फैला है। $explainHi

2. अभी क्या करें:
• तत्काल कदम: $immediateHi
• तत्परता सलाह ($urgencyHi):
$stepsBuilder

3. सहायता कब लें:
$referralSentence
            """.trimIndent()
        }

        // English default
        val cropEn = crop.replaceFirstChar { it.uppercase() }
        val typeEn = type.lowercase()
        val severityEn = severityBand.name.lowercase().replaceFirstChar { it.uppercase() }
        val spreadEn = when (fieldSpread) {
            FieldSpread.FEW_PLANTS -> "a few plants"
            FieldSpread.SOME -> "some parts of the field"
            FieldSpread.MOST -> "most of the field"
        }
        val urgencyEn = when (urgencyLevel) {
            UrgencyLevel.EARLY_WARNING -> "Early Warning"
            UrgencyLevel.ACT_THIS_WEEK -> "Act This Week"
            UrgencyLevel.ACT_NOW -> "Act Now"
        }

        if (isNotSure) {
            val unsureText = """
1. What I see:
Your $cropEn crop symptom could not be diagnosed with certainty. ${kbEntry.explainText.en}

2. What to do now:
• Immediate step: Do not spray chemical pesticides until an accurate identification is confirmed.
• Re-take clear photos in good natural daylight from multiple affected plants.

3. When to get help:
$referralSentence
            """.trimIndent()
            return GuardrailFilter.sanitize(unsureText)
        }

        val explainEn = kbEntry.explainText.en
        val immediateEn = kbEntry.immediateStep.en
        val sevAdviceEn = kbEntry.severityAdvice[severityBand.name]?.en ?: ""

        val stepsBuilder = StringBuilder()
        when (urgencyLevel) {
            UrgencyLevel.EARLY_WARNING -> {
                if (kbEntry.treatment.natural.isNotEmpty()) {
                    stepsBuilder.append("• Natural & Field Practices:\n")
                    kbEntry.treatment.natural.take(2).forEach {
                        stepsBuilder.append("  - Step ${it.step}: ${it.action.en}\n")
                    }
                }
                stepsBuilder.append("• In the coming days: Monitor daily for any increase in lesion area or pest numbers.")
            }
            UrgencyLevel.ACT_THIS_WEEK -> {
                if (sevAdviceEn.isNotBlank()) {
                    stepsBuilder.append("• Severity guidance: $sevAdviceEn\n")
                }
                if (kbEntry.treatment.natural.isNotEmpty()) {
                    stepsBuilder.append("• Cultural step: ${kbEntry.treatment.natural.first().action.en}\n")
                }
                if (kbEntry.treatment.chemical.isNotEmpty()) {
                    val chem = kbEntry.treatment.chemical.first()
                    stepsBuilder.append("• CIB&RC active: ${chem.activeIngredient} (${chem.productExample}) [follow product label].")
                }
            }
            UrgencyLevel.ACT_NOW -> {
                if (sevAdviceEn.isNotBlank()) {
                    stepsBuilder.append("• Urgent intervention: $sevAdviceEn\n")
                }
                if (kbEntry.treatment.chemical.isNotEmpty()) {
                    stepsBuilder.append("• Chemical management: Spray approved CIB&RC active ")
                    val chems = kbEntry.treatment.chemical.joinToString(", ") { "${it.activeIngredient} (${it.productExample})" }
                    stepsBuilder.append("$chems strictly following the product label with protective gear.\n")
                }
                stepsBuilder.append("• In the coming days: Inspect field after 48 hours to confirm disease spread has halted.")
            }
        }

        val adviceText = """
1. What I see:
Your $cropEn crop shows ${kbEntry.names.en} ($typeEn) at $severityEn severity. It is currently affecting $spreadEn. $explainEn

2. What to do now:
• Immediate step: $immediateEn
• Urgency recommendation ($urgencyEn):
$stepsBuilder

3. When to get help:
$referralSentence
        """.trimIndent()
        return GuardrailFilter.sanitize(adviceText)
    }

    /**
     * T2c: Formats the AI Advisory card text according to certainty and severity rules.
     * When certainty is NOT_SURE (or classId is "unknown"):
     * - Must NOT contain any severity line (including "Severity Guidance (MILD)").
     * - Must NOT contain any "Immediate Step" text that depends on severity.
     * - Returns strictly the verified explanation text from the Knowledge Base.
     */
    fun buildAdvisoryText(
        classId: String,
        certaintyBand: CertaintyBand,
        severityBand: SeverityBand,
        language: String,
        kbEntry: KbEntry
    ): String {
        val isNotSure = certaintyBand == CertaintyBand.NOT_SURE || classId.equals("unknown", ignoreCase = true)
        if (isNotSure) {
            return when (language.lowercase()) {
                "te" -> kbEntry.explainText.te
                "hi" -> kbEntry.explainText.hi
                else -> kbEntry.explainText.en
            }
        }

        return when (language.lowercase()) {
            "te" -> {
                val explain = kbEntry.explainText.te
                val immediate = kbEntry.immediateStep.te
                val advice = kbEntry.severityAdvice[severityBand.name]?.te ?: ""
                val immediatePart = if (immediate.isNotBlank()) "\n\nతక్షణ చర్య: $immediate" else ""
                val advicePart = if (advice.isNotBlank()) "\n\nనివారణ సలహా: $advice" else ""
                "$explain$immediatePart$advicePart"
            }
            "hi" -> {
                val explain = kbEntry.explainText.hi
                val immediate = kbEntry.immediateStep.hi
                val advice = kbEntry.severityAdvice[severityBand.name]?.hi ?: ""
                val immediatePart = if (immediate.isNotBlank()) "\n\nतत्काल कदम: $immediate" else ""
                val advicePart = if (advice.isNotBlank()) "\n\nसलाह: $advice" else ""
                "$explain$immediatePart$advicePart"
            }
            else -> {
                val explain = kbEntry.explainText.en
                val immediate = kbEntry.immediateStep.en
                val advice = kbEntry.severityAdvice[severityBand.name]?.en ?: ""
                val immediatePart = if (immediate.isNotBlank()) "\n\nImmediate Step: $immediate" else ""
                val advicePart = if (advice.isNotBlank()) "\n\nSeverity Guidance ($severityBand): $advice" else ""
                "$explain$immediatePart$advicePart"
            }
        }
    }

    /**
     * Appends missing sections from the verified offline guide when Llama generation is cut off (Task LLAMA-COMPLETE).
     */
    fun buildRemainingSections(
        trimmedText: String,
        classId: String,
        crop: String,
        diseaseName: String,
        type: String,
        severityBand: SeverityBand,
        certaintyBand: CertaintyBand,
        fieldSpread: FieldSpread,
        urgencyLevel: UrgencyLevel,
        region: String,
        language: String,
        kbEntry: KbEntry
    ): String {
        val fullGuide = buildCaseSpecificAdvisory(
            classId, crop, diseaseName, type, severityBand, certaintyBand,
            fieldSpread, urgencyLevel, region, language, kbEntry
        )
        val lower = trimmedText.lowercase()
        val hasSection3 = lower.contains("when to get help") || lower.contains("मदद कब लें") || lower.contains("सहायता कब") || lower.contains("3.")
        val hasSection2 = lower.contains("what to do now") || lower.contains("अब क्या करें") || lower.contains("2.")

        return when {
            !hasSection2 -> fullGuide
            !hasSection3 -> {
                val marker = when (language.lowercase()) {
                    "hi" -> "3. सहायता कब लें:"
                    else -> "3. When to get help:"
                }
                if (fullGuide.contains(marker)) {
                    fullGuide.substring(fullGuide.indexOf(marker)).trim()
                } else {
                    fullGuide
                }
            }
            else -> ""
        }
    }
}
