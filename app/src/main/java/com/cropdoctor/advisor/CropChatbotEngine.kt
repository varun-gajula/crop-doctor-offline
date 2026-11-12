package com.cropdoctor.advisor

import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.knowledge.KbEntry

data class ChatMessage(
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    BOT
}

object CropChatbotEngine {

    /**
     * Generates a grounded, high-expertise agricultural response offline using ICAR/ANGRAU knowledge.
     */
    fun answerQuestion(
        query: String,
        currentKbEntry: KbEntry?,
        language: String
    ): String {
        val q = query.lowercase().trim()
        val lang = language.lowercase()

        // 1. Organic / Natural remedies
        if (q.contains("organic") || q.contains("natural") || q.contains("సేంద్రీయ") || q.contains("వేప") || q.contains("जैविक") || q.contains("नीम")) {
            if (currentKbEntry != null) {
                val naturalList = currentKbEntry.treatment.natural.joinToString("\n• ") { item ->
                    item.action.forLanguage(lang)
                }
                return when (lang) {
                    "te" -> "🌱 **సేంద్రీయ నివారణ పద్ధతులు (${currentKbEntry.names.te}):**\n\n• $naturalList\n\n💡 *చిట్కా: ఎండ తీవ్రత తక్కువగా ఉన్నప్పుడు సాయంత్రం 4 గంటల తర్వాత మాత్రమే పిచికారీ చేయండి.*"
                    "hi" -> "🌱 **जैविक उपचार (${currentKbEntry.names.hi}):**\n\n• $naturalList\n\n💡 *सुझाव: शाम 4 बजे के बाद ही छिड़काव करें।* "
                    else -> "🌱 **Organic Treatments for ${currentKbEntry.names.en}:**\n\n• $naturalList\n\n💡 *Tip: Spray in late afternoon (after 4 PM) for maximum efficacy.*"
                }
            } else {
                return when (lang) {
                    "te" -> "🌱 **ప్రధాన సేంద్రీయ పద్ధతులు:**\n• వేప నూనె (10,000 ppm) 5 మి.లీ/లీటరు నీటికి.\n• జీవామృతం ఎకరాకు 200 లీటర్లు నీటితో పాటు అందించండి.\n• పుల్లటి మజ్జిగ 50 మి.లీ/లీటరు నీటికి బూజు తెగుళ్ల నివారణకు అద్భుతంగా పనిచేస్తుంది."
                    "hi" -> "🌱 **प्राकृतिक समाधान:**\n• नीम का तेल (10,000 ppm) 5 मिली/लीटर पानी।\n• 200 लीटर जीवामृत प्रति एकड़ सिंचाई के साथ दें।\n• खट्टी छाछ 50 मिली/लीटर फफूंद रोकने में सहायक है।"
                    else -> "🌱 **Standard Organic Practices:**\n• Neem Oil (10,000 ppm) @ 5ml per liter water.\n• Jeevamrutha @ 200L per acre through irrigation.\n• Fermented sour buttermilk @ 50ml/L for fungal blast and blight."
                }
            }
        }

        // 2. Chemical spray & dosages
        if (q.contains("chemical") || q.contains("spray") || q.contains("dose") || q.contains("మందు") || q.contains("స్ప్రే") || q.contains("రసాయన") || q.contains("दवा") || q.contains("मात्रा")) {
            if (currentKbEntry != null) {
                val chemList = currentKbEntry.treatment.chemical.joinToString("\n• ") { item ->
                    val phiText = if (item.phiDays != null) " [PHI: ${item.phiDays} days]" else ""
                    "${item.productExample} (${item.activeIngredient}) - ${item.dose}$phiText\n  🛡️ ${item.safetyLine.forLanguage(lang)}"
                }
                return when (lang) {
                    "te" -> "🧪 **సిఫార్సు చేయబడిన రసాయన నివారణ (${currentKbEntry.names.te}):**\n\n• $chemList\n\n⚠️ *రక్షణ జాగ్రత్త: గాలి వీచే దిశలోనే పిచికారీ చేయండి. చేతులకు గ్లౌజులు, ముఖానికి మాస్క్ తప్పనిసరి.*"
                    "hi" -> "🧪 **रासायनिक उपचार (${currentKbEntry.names.hi}):**\n\n• $chemList\n\n⚠️ *सावधानी: हवा की दिशा में ही स्प्रे करें। मास्क और दस्तानों का प्रयोग करें।*"
                    else -> "🧪 **Recommended Chemical Treatments (${currentKbEntry.names.en}):**\n\n• $chemList\n\n⚠️ *Safety Alert: Always wear protective masks & gloves, and spray in the direction of the wind.*"
                }
            }
        }

        // 3. Water and irrigation management
        if (q.contains("water") || q.contains("irrigation") || q.contains("నీరు") || q.contains("పారుదల") || q.contains("पानी") || q.contains("सिंचाई")) {
            return when (lang) {
                "te" -> "💧 **నీటి యాజమాన్య సూత్రాలు:**\n• తెగులు సోకినప్పుడు పొలంలో నిల్వ ఉన్న నీటిని వెంటనే బయటకు తీసి 2-3 రోజులు నేల ఆరేలా చూడండి.\n• వరిలో ఆల్టర్నేట్ వెట్టింగ్ & డ్రైయింగ్ (AWD) పద్ధతిని పాటించడం వల్ల బ్లైట్ తెగుళ్లు 40% తగ్గుతాయి.\n• మిరప మరియు టమోటాలో చుక్కల పద్ధతి (డ్రిప్) వాడండి; ఆకులపై నీళ్లు పడకుండా నేలకే నీరందించండి."
                "hi" -> "💧 **सिंचाई प्रबंधन:**\n• बीमारी के समय खेत में भरा पानी निकाल दें और 2-3 दिन हवा लगने दें।\n• धान में पानी सुखाकर दोबारा देने से झुलसा रोग 40% तक कम होता है।\n• मिर्च व टमाटर में ड्रिप सिंचाई अपनाएं ताकि पत्तियां सूखी रहें।"
                else -> "💧 **Irrigation Guidelines:**\n• Drain excess standing water immediately when bacterial or fungal lesions appear.\n• In Rice: Practice Alternate Wetting & Drying (AWD) to reduce humidity in the microclimate.\n• In Chilli/Tomato: Use drip irrigation. Avoid overhead watering to prevent spore dispersal."
            }
        }

        // 4. Prevention for next season / Seed treatment
        if (q.contains("prevent") || q.contains("seed") || q.contains("నివారణ") || q.contains("విత్తన") || q.contains("रोकथाम") || q.contains("बीज")) {
            return when (lang) {
                "te" -> "🛡️ **భవిష్యత్ నివారణ & విత్తన శుద్ధి:**\n• కిలో విత్తనానికి 3 గ్రాముల థైరమ్ లేదా 2.5 గ్రాముల కార్బెండజిమ్ కలిపి విత్తన శుద్ధి చేయండి.\n• జీవ నియంత్రణ కోసం కిలో విత్తనానికి 10 గ్రాముల ట్రైకోడెర్మా విరిడేతో శుద్ధి చేయడం శ్రేయస్కరం.\n• పంట మార్పిడి (Crop Rotation): పప్పుధాన్యాల పంటలతో మార్పిడి చేయడం వల్ల నేలలోని శిలీంధ్రాలు నశిస్తాయి."
                "hi" -> "🛡️ **भविष्य की रोकथाम और बीज उपचार:**\n• प्रति किलो बीज 3 ग्राम थीरम या 2.5 ग्राम कार्बेन्डाजिम से उपचारित करें।\n• जैविक रोकथाम के लिए 10 ग्राम ट्राइकोडर्मा प्रति किलो बीज में मिलाएं।\n• दलहनी फसलों के साथ फसल चक्र अपनाएं।"
                else -> "🛡️ **Prevention & Seed Treatment:**\n• Treat seeds with Thiram @ 3g/kg or Carbendazim @ 2.5g/kg seed before sowing.\n• Biological: Coat seeds with *Trichoderma viride* @ 10g/kg seed.\n• Rotate solanaceous crops (Tomato/Chilli) with pulses or cereals to break pathogen cycle."
            }
        }

        // 5. Crop specific queries (Rice, Cotton, Chilli, Tomato, Maize)
        val matchedCrop = when {
            q.contains("rice") || q.contains("వరి") || q.contains("धान") -> "rice"
            q.contains("cotton") || q.contains("పత్తి") || q.contains("कपास") -> "cotton"
            q.contains("chilli") || q.contains("మిరప") || q.contains("मिर्च") -> "chilli"
            q.contains("tomato") || q.contains("టమోటా") || q.contains("टमाटर") -> "tomato"
            q.contains("maize") || q.contains("మొక్కజొన్న") || q.contains("मक्का") -> "maize"
            else -> currentKbEntry?.crop ?: "general"
        }

        if (matchedCrop != "general") {
            val entries = KnowledgeBase.getAllEntries().filter { it.crop.equals(matchedCrop, ignoreCase = true) }
            val diseaseNames = entries.joinToString(", ") { entry ->
                when (lang) {
                    "te" -> entry.names.te
                    "hi" -> entry.names.hi
                    else -> entry.names.en
                }
            }

            return when (lang) {
                "te" -> "🌾 **${matchedCrop.uppercase()} పంట సమాచారం:**\nఈ పంటలో మేము గుర్తించగల ప్రధాన తెగుళ్లు:\n• $diseaseNames.\n\nమీరు నిర్దిష్టమైన తెగులు నివారణ లేదా ఎరువుల షెడ్యూల్ గురించి తెలుసుకోవాలనుకుంటున్నారా?"
                "hi" -> "🌾 **${matchedCrop.uppercase()} फसल परामर्श:**\nइस फसल के प्रमुख रोग जो हमारे ऐप में उपलब्ध हैं:\n• $diseaseNames.\n\nक्या आप किसी विशेष रोग की दवा या खाद की जानकारी चाहते हैं?"
                else -> "🌾 **${matchedCrop.uppercase()} Agronomy Guide:**\nOur ICAR knowledge base monitors these key conditions:\n• $diseaseNames.\n\nFeel free to ask about organic sprays, chemical dosages, or fertilizer management for any of these."
            }
        }

        // Fallback default helpful advice
        return when (lang) {
            "te" -> "నమస్కారం! నేను మీ అగ్రి AI డాక్టర్‌ని. మీరు వరి, పత్తి, మిరప, టమోటా, మొక్కజొన్న పంటల తెగుళ్ల నివారణ, సేంద్రీయ మందులు, రసాయన మోతాదులు మరియు నీటి యాజమాన్యం గురించి నన్ను అడగవచ్చు."
            "hi" -> "नमस्ते! मैं आपका एग्रीकेयर AI डॉक्टर हूँ। आप धान, कपास, मिर्च, टमाटर, और मक्का की बीमारियों, जैविक उपचार और दवा की मात्रा के बारे में मुझसे पूछ सकते हैं।"
            else -> "Hello! I am your AgriCare AI Doctor. Ask me anything about crop diseases, organic solutions (Neem oil, Jeevamrutha), chemical dosages (CIB&RC approved), or water and fertilizer schedules for Rice, Cotton, Chilli, Tomato, and Maize."
        }
    }
}
