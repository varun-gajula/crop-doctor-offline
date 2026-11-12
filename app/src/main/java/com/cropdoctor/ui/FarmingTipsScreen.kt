package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

data class DetailedTip(
    val crop: String,
    val source: String,
    val titleEn: String,
    val titleTe: String,
    val titleHi: String,
    val contentEn: String,
    val contentTe: String,
    val contentHi: String,
    val takeawayEn: String,
    val takeawayTe: String,
    val takeawayHi: String
)

@Composable
fun FarmingTipsScreen(
    language: String,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedCropFilter by remember { mutableStateOf("all") }

    val screenTitle = when (language) {
        "te" -> "విశ్వవిద్యాలయ వ్యవసాయ చిట్కాలు"
        "hi" -> "कृषि विश्वविद्यालय सुझाव"
        else -> "ANGRAU & ICAR Farming Tips"
    }

    val crops = listOf(
        Pair("all", when (language) { "te" -> "అన్నీ"; "hi" -> "सभी"; else -> "All Crops" }),
        Pair("rice", "🌾 Rice"),
        Pair("cotton", "☁️ Cotton"),
        Pair("chilli", "🌶️ Chilli"),
        Pair("tomato", "🍅 Tomato"),
        Pair("maize", "🌽 Maize")
    )

    val tips = listOf(
        // RICE TIPS
        DetailedTip(
            crop = "rice",
            source = "ANGRAU & ICAR-IIRR Hyderabad",
            titleEn = "🌾 Rice: Alternate Wetting and Drying (AWD)",
            titleTe = "🌾 వరి: ఆల్టర్నేట్ వెట్టింగ్ & డ్రైయింగ్ (నీటి యాజమాన్యం)",
            titleHi = "🌾 धान: रुक-रुक कर सिंचाई (AWD तकनीक)",
            contentEn = "Continuous flooding creates high humidity microclimates that trigger Bacterial Leaf Blight and Sheath Blight. Drain water until fine hairline soil cracks appear before reflooding. This reduces blast by 35% and cuts methane emissions.",
            contentTe = "మడిలో ఎల్లప్పుడూ నీరు నిల్వ ఉంచడం వల్ల గాలిలో తేమ పెరిగి బ్యాక్టీరియల్ బ్లైట్ మరియు షీత్ బ్లైట్ తెగుళ్లు విజృంభిస్తాయి. నేలపై చిన్న పగుళ్లు వచ్చే వరకు నీటిని తీసి ఆరబెట్టి, ఆపై తిరిగి నీరు పెట్టడం ద్వారా తెగుళ్లు 35% తగ్గుతాయి.",
            contentHi = "खेत में लगातार पानी भरा रहने से फफूंद और जीवाणु झुलसा तेजी से फैलता है। खेत का पानी सुखाकर 2-3 दिन हवा लगने दें, फिर दोबारा पानी दें। इससे झुलसा रोग 35% कम होता है।",
            takeawayEn = "💡 Saves 30% water and prevents bacterial sheath infections.",
            takeawayTe = "💡 30% నీరు ఆదా అవ్వడమే కాక తెగుళ్లు రాకుండా కాపాడుతుంది.",
            takeawayHi = "💡 30% पानी की बचत और झुलसा रोग से सुरक्षा।"
        ),
        DetailedTip(
            crop = "rice",
            source = "ICAR-IIRR & Directorate of Rice Research",
            titleEn = "🌾 Rice: Split Nitrogen & Potash Fertilizer Dosing",
            titleTe = "🌾 వరి: నత్రజని, పొటాష్ ఎరువుల సమతుల్య యాజమాన్యం",
            titleHi = "🌾 धान: संतुलित यूरिया व पोटाश प्रबंधन",
            contentEn = "Excessive single-dose Urea makes rice blades succulent and highly prone to Neck Blast. Split Nitrogen into 3 equal doses (Basal, Tillering, Panicle Initiation). Always apply MOP Potash (30kg/acre) to harden epidermal leaf cell walls.",
            contentTe = "ఒకేసారి ఎక్కువ యూరియా వేయడం వల్ల ఆకులు మెత్తబడి అగ్గి తెగులు (బ్లాస్ట్) వేగంగా సోకుతుంది. నత్రజనిని మూడు దఫాలుగా వేయండి. ఎకరానికి 30 కిలోల పొటాష్ ఎరువును తప్పనిసరిగా వేయడం వల్ల ఆకుల కణజాలం గట్టిపడి తెగుళ్లను తట్టుకుంటుంది.",
            contentHi = "एक साथ अधिक यूरिया डालने से पत्तियां कोमल होकर ब्लास्ट रोग की चपेट में आ जाती हैं। यूरिया 3 भागों में दें और पोटाश 30 किग्रा/एकड़ अवश्य डालें ताकि पत्तियां मजबूत बनें।",
            takeawayEn = "💡 Balanced potash strengthens leaf resistance against fungal penetration.",
            takeawayTe = "💡 పొటాష్ వేయడం వల్ల ఆకులు గట్టిపడి శిలీంధ్రాలను నిరోధిస్తాయి.",
            takeawayHi = "💡 पोटाश पत्तियों की रोग प्रतिरोधक क्षमता बढ़ाता है।"
        ),

        // COTTON TIPS
        DetailedTip(
            crop = "cotton",
            source = "ICAR-CICR Nagpur & ANGRAU Guntur",
            titleEn = "☁️ Cotton: Integrated Pest Management & Yellow Traps",
            titleTe = "☁️ పత్తి: సమగ్ర సస్యరక్షణ & పసుపు జిగురు అట్టలు",
            titleHi = "☁️ कपास: एकीकृत कीट प्रबंधन (IPM) व पीले कार्ड",
            contentEn = "Install 15-20 yellow sticky cards per acre at canopy height to capture whiteflies and jassids before they transmit leaf curl virus. Plant castor and marigold as border trap crops to divert Spodoptera larvae.",
            contentTe = "ఎకరాకు 15-20 పసుపు జిగురు అట్టలను పైరు ఎత్తులో అమర్చడం వల్ల తెల్లదోమ మరియు పచ్చదోమలు చిక్కుకుని ఆకుముడత వైరస్ వ్యాపించకుండా ఆగుతుంది. చేను చుట్టూ ఆముదం మరియు బంతి మొక్కలను ఎర పంటలుగా వేయండి.",
            contentHi = "प्रति एकड़ 15-20 पीले चिपचिपे कार्ड लगाएं जिससे सफेद मक्खी और हरा तेला चिपक जाएं और वायरस न फैले। खेत की मेड़ों पर अरंडी व गेंदा लगाएं।",
            takeawayEn = "💡 Reduces pesticide costs by 45% and prevents Leaf Curl Virus.",
            takeawayTe = "💡 పురుగుమందుల ఖర్చు 45% తగ్గి ఆకుముడత వైరస్ రాకుండా ఆపుతుంది.",
            takeawayHi = "💡 45% कीटनाशक खर्च की बचत और वायरस की रोकथाम।"
        ),
        DetailedTip(
            crop = "cotton",
            source = "ANGRAU Lam Farm & UAS Raichur",
            titleEn = "☁️ Cotton: Foliar Spray Against Leaf Reddening",
            titleTe = "☁️ పత్తి: ఆకులు ఎర్రబడటం నివారణకు పోషక పిచికారీ",
            titleHi = "☁️ कपास: पत्तियां लाल होने से रोकने के उपाय",
            contentEn = "During flowering and boll development stages, magnesium deficiency causes leaves to turn purple-red and shed. Spray 1% Magnesium Sulphate + 1% Urea (10g MgSO4 + 10g Urea per liter water) twice at 15-day intervals.",
            contentTe = "పూత మరియు కాయ దశలో మెగ్నీషియం లోపం వల్ల ఆకులు ఎర్రబడి రాలిపోతాయి. నివారణకు లీటరు నీటికి 10 గ్రాముల మెగ్నీషియం సల్ఫేట్ + 10 గ్రాముల యూరియా కలిపి 15 రోజుల వ్యవధిలో రెండుసార్లు పిచికారీ చేయండి.",
            contentHi = "फूल व गूलर बनते समय मैग्नीशियम की कमी से पत्तियां लाल होकर गिरती हैं। प्रति लीटर पानी 10 ग्राम मैग्नीशियम सल्फेट + 10 ग्राम यूरिया का छिड़काव 15 दिन के अंतर पर करें।",
            takeawayEn = "💡 Prevents boll drop and maintains active photosynthesis.",
            takeawayTe = "💡 పూత, కాయ రాలకుండా ఆపి అధిక దిగుబడిని అందిస్తుంది.",
            takeawayHi = "💡 गूलर झड़ने से रोकता है और उपज बढ़ाता है।"
        ),

        // CHILLI TIPS
        DetailedTip(
            crop = "chilli",
            source = "ANGRAU Lam Farm Guntur & IIHR Bengaluru",
            titleEn = "🌶️ Chilli: Barrier Border Cropping Against Thrips & Mites",
            titleTe = "🌶️ మిరప: తామర పురుగులు, నల్లి నివారణకు సరిహద్దు పంటలు",
            titleHi = "🌶️ मिर्च: थ्रिप्स व माइट से बचाव के लिए अवरोधक फसलें",
            contentEn = "Sow 4 dense border rows of Maize or Sorghum around the chilli plot 15 days before transplanting. These tall crops act as physical wind barriers, stopping incoming viruliferous thrips and keeping leaf curl below economic thresholds.",
            contentTe = "మిరప నాటడానికి 15 రోజుల ముందే చేను చుట్టూ 4 వరుసల మొక్కజొన్న లేదా జొన్న పంటను దట్టంగా వేయండి. ఇవి ఎత్తైన రక్షణ గోడలా పనిచేసి గాలి ద్వారా వచ్చే తామర పురుగులను మరియు నల్లిని అడ్డుకుంటాయి.",
            contentHi = "मिर्च रोपाई से 15 दिन पहले खेत के चारों ओर 4 कतारों में मक्का या ज्वार लगाएं। यह प्राकृतिक दीवार थ्रिप्स और कीटों को मिर्च के पौधों तक पहुंचने से रोकती है।",
            takeawayEn = "💡 Natural physical barrier preventing Gemini & Tospo virus epidemics.",
            takeawayTe = "💡 సహజ రక్షణ కవచం ద్వారా ఆకుముడత వైరస్ రాకుండా కాపాడుతుంది.",
            takeawayHi = "💡 वायरस जनित मरोड़िया रोग से प्राकृतिक सुरक्षा।"
        ),
        DetailedTip(
            crop = "chilli",
            source = "IIHR Bengaluru & TNAU Coimbatore",
            titleEn = "🌶️ Chilli: Die-Back and Fruit Rot (Anthracnose) Prevention",
            titleTe = "🌶️ మిరప: కొమ్మ ఎండు & కాయకుళ్లు (ఆంత్రక్నోస్) నివారణ",
            titleHi = "🌶️ मिर्च: फल सड़न व डाइबैक (एंथ्रेक्नोज) रोकथाम",
            contentEn = "Seed treatment with Trichoderma viride @ 10g/kg seed. At flowering and fruit initiation, apply prophylactic spray of Azoxystrobin 23% SC @ 1ml/L or Copper Oxychloride @ 3g/L. Avoid high nitrogen late in the crop.",
            contentTe = "విత్తనాలను కిలోకు 10 గ్రాముల ట్రైకోడెర్మా విరిడేతో శుద్ధి చేయండి. పూత మరియు కాయ దశలో అజోక్సిస్ట్రోబిన్ 1 మి.లీ లేదా కాపర్ ఆక్సీక్లోరైడ్ 3 గ్రాములు లీటరు నీటికి కలిపి పిచికారీ చేయడం వల్ల కాయకుళ్లు రాదు.",
            contentHi = "बीज को 10 ग्राम ट्राइकोडर्मा प्रति किलो से उपचारित करें। फूल आते समय एज़ोक्सीस्ट्रोबिन 1 मिली/लीटर या कॉपर ऑक्सीक्लोराइड 3 ग्राम/लीटर का छिड़काव करें।",
            takeawayEn = "💡 Prevents 40% fruit loss and black spots on drying pods.",
            takeawayTe = "💡 ఎండు మిరపపై నల్లటి మచ్చలు రాకుండా రంగు, నాణ్యత కాపాడుతుంది.",
            takeawayHi = "💡 फलों पर काले धब्बे और सड़न से पूर्ण बचाव।"
        ),

        // TOMATO TIPS
        DetailedTip(
            crop = "tomato",
            source = "ICAR-IIHR Bengaluru & ANGRAU",
            titleEn = "🍅 Tomato: Staking & Trellising Against Blights",
            titleTe = "🍅 టమోటా: కర్రల ఊతం (స్టేకింగ్) & ఆకుల సంరక్షణ",
            titleHi = "🍅 टमाटर: बांस की खपच्चियों से सहारा (स्टेकिंग)",
            contentEn = "Stake tomato plants on bamboo poles with twine 25-30 days after transplanting. Keeping leaves 30 cm above soil prevents soil-borne Early and Late Blight spores from splashing onto foliage during rainfall.",
            contentTe = "నాటిన 25-30 రోజులకు వెదురు కర్రలు మరియు తాళ్లతో మొక్కలను పైకి కట్టండి. ఆకులు నేలమట్టానికి 30 సెం.మీ ఎత్తులో ఉండటం వల్ల వర్షపు చినుకుల ద్వారా నేలలోని శిలీంధ్రాలు ఆకులకు సోకకుండా ఆకుమాడు తెగుళ్లు ఆగుతాయి.",
            contentHi = "रोपाई के 25-30 दिन बाद पौधों को बांस की खपच्चियों से बांधें। पत्तियां जमीन से 30 सेमी ऊपर रहने से मिट्टी की फफूंद पत्तियों पर नहीं पहुंचती और झुलसा रोग नहीं लगता।",
            takeawayEn = "💡 Extends harvest by 4 weeks and reduces fungal blight by 50%.",
            takeawayTe = "💡 దిగుబడి కాలాన్ని 4 వారాలు పొడిగించి ఆకుమాడు తెగుళ్లను 50% తగ్గిస్తుంది.",
            takeawayHi = "💡 4 सप्ताह अतिरिक्त पैदावार और झुलसा रोग में 50% कमी।"
        ),
        DetailedTip(
            crop = "tomato",
            source = "ANGRAU & IIHR Horticulture Handbook",
            titleEn = "🍅 Tomato: Blossom End Rot & Cracking Prevention",
            titleTe = "🍅 టమోటా: కాయ పిక్క కుళ్లు & పగుళ్లు రాకుండా జాగ్రత్తలు",
            titleHi = "🍅 टमाटर: फल फटने व काला धब्बा रोकने के उपाय",
            contentEn = "Blossom end rot is caused by Calcium deficiency and erratic irrigation. Spray Calcium Nitrate @ 5g/L + Boron 20% @ 1g/L at peak fruit setting. Maintain uniform soil moisture with drip lines.",
            contentTe = "టమోటా కాయ కింది భాగం నల్లబడి కుళ్ళిపోవడానికి కాల్షియం లోపం మరియు నీటి ఎద్దడి కారణం. కాయ దశలో లీటరు నీటికి 5 గ్రాముల కాల్షియం నైట్రేట్ + 1 గ్రాము బోరాన్ కలిపి పిచికారీ చేయండి.",
            contentHi = "फल का निचला हिस्सा काला होना कैल्शियम की कमी का लक्षण है। प्रति लीटर पानी 5 ग्राम कैल्शियम नाइट्रेट + 1 ग्राम बोरॉन का छिड़काव करें और नियमित नमी बनाए रखें।",
            takeawayEn = "💡 Produces smooth, firm, crack-free fruits with excellent market value.",
            takeawayTe = "💡 కాయలు పగలకుండా నిగనిగలాడే నాణ్యతతో మార్కెట్ ధరను పెంచుతుంది.",
            takeawayHi = "💡 चमकदार, ठोस और बिना दरार वाले उत्तम फल प्राप्त होते हैं।"
        ),

        // MAIZE TIPS
        DetailedTip(
            crop = "maize",
            source = "ICAR-IIMR Hyderabad & ANGRAU",
            titleEn = "🌽 Maize: Fall Armyworm (FAW) Whorl Application",
            titleTe = "🌽 మొక్కజొన్న: కత్తెర పురుగు నివారణకు సుడి యాజమాన్యం",
            titleHi = "🌽 मक्का: फाल आर्मीवर्म (FAW) का पोंगा उपचार",
            contentEn = "Early instar FAW larvae feed inside whorls. Apply dry sand mixed with neem seed powder (9:1 ratio) directly into plant whorls at 15-20 days. For severe infestation, apply Emamectin Benzoate 5% SG @ 0.4g/L directed into whorls.",
            contentTe = "కత్తెర పురుగు లద్దెలు సుడుల లోపల దాక్కుని తింటాయి. నాటిన 15-20 రోజులకు సుడులలో పొడి ఇసుక మరియు వేప పిండి మిశ్రమాన్ని (9:1 నిష్పత్తిలో) వేయండి. ఉధృతి ఎక్కువగా ఉంటే ఇమామెక్టిన్ బెంజోయేట్ 0.4 గ్రాములు లీటరుకు సుడులు తడిచేలా పిచికారీ చేయండి.",
            contentHi = "फाल आर्मीवर्म सुंडी पोंगे के अंदर रहती है। 15-20 दिन की फसल में सूखी रेत और नीम की खली (9:1) का मिश्रण पोंगे में डालें। अधिक प्रकोप में इमामेक्टिन बेंजोएट 0.4 ग्राम/लीटर का छिड़काव करें।",
            takeawayEn = "💡 Suffocates young larvae inside whorls with zero environmental damage.",
            takeawayTe = "💡 సుడులలోని లేత పురుగులను సహజంగా ఊపిరాడకుండా చేసి చంపుతుంది.",
            takeawayHi = "💡 पोंगे के अंदर छुपी सुंडियों का शत-प्रतिशत प्राकृतिक खात्मा।"
        )
    )

    val filteredTips = if (selectedCropFilter == "all") tips else tips.filter { it.crop == selectedCropFilter }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FarmGreenPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = screenTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ICAR • ANGRAU • TNAU Agronomy Manuals",
                        fontSize = 11.sp,
                        color = Color(0xFFC8E6C9)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAF7))
                .padding(paddingValues)
        ) {
            // Horizontal Crop Selector Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                crops.forEach { (cropKey, cropLabel) ->
                    val isSelected = selectedCropFilter == cropKey
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) FarmGreenPrimary else Color(0xFFE8F5E9),
                        modifier = Modifier.clickable { selectedCropFilter = cropKey }
                    ) {
                        Text(
                            text = cropLabel,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else FarmGreenDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Detailed Tips Cards
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                filteredTips.forEach { tip ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Title in selected language
                            Text(
                                text = when (language) {
                                    "te" -> tip.titleTe
                                    "hi" -> tip.titleHi
                                    else -> tip.titleEn
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark,
                                lineHeight = 24.sp
                            )

                            // Authority Citation Badge
                            Text(
                                text = "🏛️ Source: " + tip.source,
                                fontSize = 12.sp,
                                color = Color(0xFF1565C0),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                            )

                            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Main Detailed Advisory Content in selected language
                            Text(
                                text = when (language) {
                                    "te" -> tip.contentTe
                                    "hi" -> tip.contentHi
                                    else -> tip.contentEn
                                },
                                fontSize = 15.sp,
                                color = Color(0xFF37474F),
                                lineHeight = 23.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Takeaway Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when (language) {
                                        "te" -> tip.takeawayTe
                                        "hi" -> tip.takeawayHi
                                        else -> tip.takeawayEn
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
