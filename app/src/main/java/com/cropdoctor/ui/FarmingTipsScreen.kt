package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun FarmingTipsScreen(
    language: String,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val screenTitle = when (language) {
        "te" -> "వ్యవసాయ చిట్కాలు"
        "hi" -> "कृषि सुझाव"
        else -> "Farming Tips"
    }

    val tips = listOf(
        Triple(
            "🌾 Rice Water Management",
            "వరిలో నీటి యాజమాన్యం: పొలంలో నీటిని ఎప్పటికప్పుడు తీసి ఆరబెట్టడం ద్వారా బ్యాక్టీరియల్ బ్లైట్ తెగులును తగ్గించవచ్చు.",
            "धान में जल प्रबंधन: खेत का पानी समय-समय पर निकालने से जीवाणु झुलसा रोग कम फैलता है।"
        ),
        Triple(
            "☁️ Cotton IPM & Traps",
            "పత్తిలో లింగాకర్షక బుట్టలు: ఎకరాకు 15-20 పసుపు జిగురు అట్టలను అమర్చడం ద్వారా తెల్లదోమను అరికట్టవచ్చు.",
            "कपास में पीले कार्ड: प्रति एकड़ 15-20 पीले चिपचिपे कार्ड लगाने से सफेद मक्खी नियंत्रित होती है।"
        ),
        Triple(
            "🌶️ Chilli Border Crops",
            "మిరప తోట రక్షణ: మిరప చుట్టూ 4 వరుసల మొక్కజొన్న లేదా జొన్న పంటను రక్షణ కవచంగా వేయండి.",
            "मिर्च में अवरोधक फसलें: खेत के चारों ओर 4 कतारों में मक्का या ज्वार लगाने से कीट रुकते हैं।"
        ),
        Triple(
            "🍅 Tomato Staking & Aeration",
            "టమాటాలో కర్రల ఊతం: మొక్కలను వెదురు కర్రలతో కట్టడం ద్వారా ఆకులు నేలకు తగలకుండా తెగుళ్లను నివారించవచ్చు.",
            "टमाटर को सहारा: बांस की खपच्चियों से पौधों को सहारा देने से पत्तियां जमीन से ऊपर रहती हैं।"
        ),
        Triple(
            "🌽 Maize FAW Whorl Sand",
            "మొక్కజొన్న కత్తెర పురుగు: సుడులలో ఇసుక మరియు వేప పిండి మిశ్రమాన్ని వేసి లేత పురుగులను చంపండి.",
            "मक्का का फाल आर्मीवर्म: पौधों के पोंगे में सूखी रेत और राख का मिश्रण डालने से सूंडियां मर जाती हैं।"
        )
    )

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
                Text(
                    text = screenTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAF7))
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            tips.forEach { (heading, teContent, hiContent) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = heading,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (language) { "te" -> teContent; "hi" -> hiContent; else -> teContent },
                            fontSize = 15.sp,
                            color = Color(0xFF37474F),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

