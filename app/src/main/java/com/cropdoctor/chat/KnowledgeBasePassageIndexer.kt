package com.cropdoctor.chat

import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBaseRoot

/**
 * Extracts and structures discrete passages from KnowledgeBaseRoot entries
 * across English, Telugu, and Hindi for SQLite FTS5 storage and retrieval.
 */
object KnowledgeBasePassageIndexer {

    fun buildAllPassages(root: KnowledgeBaseRoot): List<Passage> {
        val passages = mutableListOf<Passage>()
        val languages = listOf("en", "te", "hi")

        for (entry in root.classes) {
            for (lang in languages) {
                passages.addAll(buildPassagesForClassAndLanguage(entry, lang))
            }
        }
        return passages
    }

    fun buildPassagesForClassAndLanguage(entry: KbEntry, lang: String): List<Passage> {
        val classId = entry.classId
        val isHealthy = entry.type.equals("healthy", ignoreCase = true)
        val defaultSource = entry.treatment.chemical.firstOrNull()?.source
            ?: entry.treatment.natural.firstOrNull()?.source
            ?: entry.getRegionalAdvice("telangana").source

        val list = mutableListOf<Passage>()

        // 1. What is it (overview, cause, nature)
        val whatIsItText = entry.explainText.forLanguage(lang).ifBlank {
            when (lang) {
                "te" -> "${entry.names.te} గురించి సంక్షిప్త వివరణ."
                "hi" -> "${entry.names.hi} का परिचय और विवरण।"
                else -> "Overview and identity of ${entry.names.en}."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.WHAT_IS_IT}",
                classId = classId,
                section = QuestionSection.WHAT_IS_IT,
                language = lang,
                text = whatIsItText,
                source = defaultSource
            )
        )

        // 2. Symptoms & Early Warning
        val warning = entry.earlyWarningSigns.forLanguage(lang)
        val sevMild = entry.severityAdvice["MILD"]?.forLanguage(lang) ?: ""
        val sevMod = entry.severityAdvice["MODERATE"]?.forLanguage(lang) ?: ""
        val symptomsText = buildString {
            if (warning.isNotBlank()) append(warning)
            if (sevMod.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(sevMod)
            } else if (sevMild.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(sevMild)
            }
        }.ifBlank {
            when (lang) {
                "te" -> "ఈ పరిస్థితి యొక్క ముఖ్య లక్షణాలు."
                "hi" -> "इस समस्या के मुख्य लक्षण और पहचान।"
                else -> "Key symptoms and early warning signs."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.SYMPTOMS}",
                classId = classId,
                section = QuestionSection.SYMPTOMS,
                language = lang,
                text = symptomsText,
                source = defaultSource
            )
        )

        // 3. What to do now (Immediate step)
        val immediateText = entry.immediateStep.forLanguage(lang).ifBlank {
            when (lang) {
                "te" -> "పొలంలో వెంటనే తీసుకోవలసిన చర్యలు."
                "hi" -> "खेत में तत्काल उठाए जाने वाले कदम।"
                else -> "Immediate first response actions in the field."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.WHAT_TO_DO_NOW}",
                classId = classId,
                section = QuestionSection.WHAT_TO_DO_NOW,
                language = lang,
                text = immediateText,
                source = defaultSource
            )
        )

        // 4. Prevention and field practice
        val prevText = entry.preventionAndFieldPractice.forLanguage(lang).ifBlank {
            when (lang) {
                "te" -> "భవిష్యత్తులో తెగులు రాకుండా నివారణ పద్ధతులు మరియు సేద్య యాజమాన్యం."
                "hi" -> "फसल में रोकथाम के उपाय और कृषि प्रबंधन।"
                else -> "Preventative field practices, sanitation, and crop management."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.PREVENTION}",
                classId = classId,
                section = QuestionSection.PREVENTION,
                language = lang,
                text = prevText,
                source = defaultSource
            )
        )

        // 5. Natural and traditional remedies
        val naturalSource = entry.treatment.natural.firstOrNull()?.source ?: defaultSource
        val naturalText = if (entry.treatment.natural.isNotEmpty()) {
            entry.treatment.natural.joinToString("\n") { step ->
                val act = step.action.forLanguage(lang)
                "• Step ${step.step}: $act"
            }
        } else if (isHealthy) {
            when (lang) {
                "te" -> "ఆరోగ్యకరమైన పంట: సహజ పద్ధతులతో నేల సారాన్ని, సమతుల్య ఎరువులను కొనసాగించండి."
                "hi" -> "स्वस्थ फसल: प्राकृतिक तरीकों से मिट्टी की उर्वरता और संतुलित पोषण बनाए रखें।"
                else -> "Healthy crop: Maintain soil health and balanced nutrients through good organic practices."
            }
        } else {
            when (lang) {
                "te" -> "సేంద్రీయ పద్ధతులు మరియు వేప కషాయం ద్వారా ప్రారంభ నివారణ చేపట్టండి."
                "hi" -> "जैविक उपचार और नीम आधारित घोल से प्रारंभिक रोकथाम करें।"
                else -> "Apply cultural practices and botanical sprays for biological suppression."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.NATURAL_REMEDY}",
                classId = classId,
                section = QuestionSection.NATURAL_REMEDY,
                language = lang,
                text = naturalText,
                source = naturalSource
            )
        )

        // 6. Chemical treatment
        val chemSource = entry.treatment.chemical.firstOrNull()?.source ?: "CIB&RC Registered Uses"
        val chemText = if (entry.treatment.chemical.isNotEmpty()) {
            entry.treatment.chemical.joinToString("\n") { ch ->
                val safe = ch.safetyLine.forLanguage(lang)
                val phi = if (ch.phiDays != null) " (PHI: ${ch.phiDays} days)" else ""
                "• ${ch.activeIngredient} (${ch.productExample}) - Dose: ${ch.dose}$phi. $safe"
            }
        } else if (isHealthy) {
            when (lang) {
                "te" -> "ఆరోగ్యకరమైన పంట: ఎటువంటి రసాయన శిలీంధ్రనాశినులు లేదా పురుగుమందుల అవసరం లేదు."
                "hi" -> "स्वस्थ फसल: किसी भी रासायनिक कीटनाशक या फफूंदनाशक की आवश्यकता नहीं है।"
                else -> "Healthy crop: No chemical fungicides or insecticides required. Protect crop health naturally."
            }
        } else {
            when (lang) {
                "te" -> "CIB&RC సిఫార్సు చేసిన రసాయన మందులను లేబుల్ సూచనల ప్రకారం వాడండి."
                "hi" -> "CIB&RC अनुशंसित रासायनिक दवाओं का उपयोग उत्पाद लेबल के अनुसार करें।"
                else -> "Apply CIB&RC registered products strictly according to the product label."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.CHEMICAL}",
                classId = classId,
                section = QuestionSection.CHEMICAL,
                language = lang,
                text = chemText,
                source = chemSource
            )
        )

        // 7. Safety and waiting period (PHI)
        val safetySource = entry.treatment.chemical.firstOrNull()?.source ?: "CIB&RC Safety Standards"
        val safetyText = if (entry.treatment.chemical.isNotEmpty()) {
            entry.treatment.chemical.joinToString("\n") { ch ->
                val safe = ch.safetyLine.forLanguage(lang)
                val phiDays = ch.phiDays
                val phiStr = if (phiDays != null) {
                    when (lang) {
                        "te" -> "కోతకు ముందు వేచి ఉండే సమయం (PHI): $phiDays రోజులు."
                        "hi" -> "तुड़ाई/कटाई से पहले प्रतीक्षा अवधि (PHI): $phiDays दिन।"
                        else -> "Pre-harvest interval (PHI): $phiDays days before harvest."
                    }
                } else {
                    when (lang) {
                        "te" -> "కోత సమయం కోసం ఉత్పత్తి లేబుల్ పాటించండి."
                        "hi" -> "कटाई समय हेतु उत्पाद लेबल पढ़ें।"
                        else -> "Follow the product label for harvest waiting interval."
                    }
                }
                "• $safe $phiStr"
            }
        } else if (isHealthy) {
            when (lang) {
                "te" -> "ఆరోగ్యకరమైన పంటపై రసాయనాలు వాడనందున ఎటువంటి వేచి ఉండే సమయం అవసరం లేదు."
                "hi" -> "स्वस्थ फसल पर किसी रसायन का प्रयोग न होने से कोई प्रतीक्षा अवधि आवश्यक नहीं है।"
                else -> "No chemical safety waiting period required for healthy crops."
            }
        } else {
            when (lang) {
                "te" -> "మందులు చల్లేటప్పుడు మాస్క్ మరియు గ్లౌజులు ధరించండి. లేబుల్ పాటించండి."
                "hi" -> "छिड़काव करते समय मास्क और दस्ताने पहनें। उत्पाद लेबल का पालन करें।"
                else -> "Wear protective clothing, gloves, and mask. Always follow product label instructions."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.SAFETY_WAITING_PERIOD}",
                classId = classId,
                section = QuestionSection.SAFETY_WAITING_PERIOD,
                language = lang,
                text = safetyText,
                source = safetySource
            )
        )

        // 8. When to call the extension officer
        val officerText = entry.whenToCallExtensionOfficer.forLanguage(lang).ifBlank {
            when (lang) {
                "te" -> "పరిస్థితి తీవ్రమైతే వెంటనే వ్యవసాయ విస్తరణ అధికారిని లేదా కిసాన్ కాల్ సెంటర్ (1800-180-1551) ను సంప్రదించండి."
                "hi" -> "गंभीर स्थिति होने पर तुरंत कृषि विस्तार अधिकारी या किसान कॉल सेंटर (1800-180-1551) पर कॉल करें।"
                else -> "Call the agriculture extension officer or Kisan Call Centre (Toll-Free 1800-180-1551) if condition worsens."
            }
        }
        list.add(
            Passage(
                id = "${classId}_${lang}_${QuestionSection.WHEN_TO_CALL_OFFICER}",
                classId = classId,
                section = QuestionSection.WHEN_TO_CALL_OFFICER,
                language = lang,
                text = officerText,
                source = defaultSource
            )
        )

        return list
    }
}
