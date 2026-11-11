package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.domain.TreatmentSelector
import com.cropdoctor.domain.TreatmentSelector.TreatmentTab
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun ResultScreen(
    result: ClassifierResult,
    kbEntry: KbEntry,
    language: String,
    advisoryText: String,
    isGeneratingAdvice: Boolean,
    onBackClick: () -> Unit,
    onChangeLanguageClick: () -> Unit,
    onViewMoreDetails: () -> Unit
) {
    val scrollState = rememberScrollState()
    val initialTab = TreatmentSelector.getInitialTab(result.severityBand, result.classId)
    var selectedTreatmentTab by remember(initialTab) { mutableStateOf(initialTab) }

    val shouldRefer = RulesEngine.shouldReferToKVK(result.severityBand, kbEntry.referralThreshold)
    val isHealthy = result.classId.endsWith("_healthy")

    val screenTitle = when (language) {
        "te" -> "ఫలితాలు"
        "hi" -> "परिणाम"
        else -> "Result"
    }

    val alertBannerText = if (isHealthy) {
        when (language) {
            "te" -> "✓ ఆరోగ్యకరమైన పంట!"
            "hi" -> "✓ स्वस्थ फसल!"
            else -> "✓ Healthy Crop!"
        }
    } else {
        when (language) {
            "te" -> "🚨 తెగులు గుర్తించబడింది!"
            "hi" -> "🚨 रोग पाया गया!"
            else -> "🚨 Disease Detected!"
        }
    }

    val confidencePercent = (result.confidence * 100).toInt().coerceIn(60, 99)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FarmGreenPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                // Small language toggle button on top right corner
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onChangeLanguageClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = language.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Leaf Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🍃", fontSize = 84.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alert Banner (Red for disease, Green for healthy)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = alertBannerText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHealthy) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Disease Title & Crop
            Text(
                text = kbEntry.names.forLanguage(language),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FarmGreenDark,
                lineHeight = 30.sp
            )

            Text(
                text = "(${kbEntry.crop.replaceFirstChar { it.uppercase() }})",
                fontSize = 17.sp,
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Confidence Progress Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (language) { "te" -> "ఖచ్చితత్వం:"; "hi" -> "सटीकता:"; else -> "Confidence:" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "$confidencePercent%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { confidencePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = FarmGreenPrimary,
                        trackColor = Color(0xFFE0E0E0),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About the Disease Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (language) { "te" -> "వ్యాధి వివరాలు"; "hi" -> "रोग के बारे में"; else -> "About the Disease" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = kbEntry.explainText.forLanguage(language),
                        fontSize = 15.sp,
                        color = Color(0xFF424242),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Treatment Suggestions Section (with Natural / Chemical toggle)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (language) { "te" -> "చికిత్స సూచనలు"; "hi" -> "उपचार सुझाव"; else -> "Treatment Suggestions" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5E9), RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { selectedTreatmentTab = TreatmentTab.NATURAL },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTreatmentTab == TreatmentTab.NATURAL) FarmGreenPrimary else Color.Transparent,
                                contentColor = if (selectedTreatmentTab == TreatmentTab.NATURAL) Color.White else Color(0xFF333333)
                            ),
                            elevation = null
                        ) {
                            Text(
                                text = when (language) { "te" -> "🌿 సహజ నివారణ"; "hi" -> "🌿 जैविक"; else -> "Natural" },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { selectedTreatmentTab = TreatmentTab.CHEMICAL },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTreatmentTab == TreatmentTab.CHEMICAL) Color(0xFF1565C0) else Color.Transparent,
                                contentColor = if (selectedTreatmentTab == TreatmentTab.CHEMICAL) Color.White else Color(0xFF333333)
                            ),
                            elevation = null
                        ) {
                            Text(
                                text = when (language) { "te" -> "🧪 రసాయన మందులు"; "hi" -> "🧪 रासायनिक"; else -> "Chemical" },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Treatment Steps with Checkmarks (Matching sample photo)
                    if (selectedTreatmentTab == TreatmentTab.NATURAL) {
                        kbEntry.treatment.natural.forEach { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = FarmGreenPrimary,
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = step.action.forLanguage(language),
                                    fontSize = 15.sp,
                                    color = Color(0xFF333333),
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    } else {
                        if (kbEntry.treatment.chemical.isEmpty()) {
                            Text(
                                text = when (language) {
                                    "te" -> "రసాయన మందులు అవసరం లేదు."
                                    "hi" -> "रासायनिक दवा की आवश्यकता नहीं है।"
                                    else -> "No chemical application required."
                                },
                                fontSize = 15.sp,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            kbEntry.treatment.chemical.forEach { chem ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF1565C0),
                                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = chem.activeIngredient,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0D47A1)
                                            )
                                            Text(
                                                text = "Safety: " + chem.safetyLine.forLanguage(language),
                                                fontSize = 13.sp,
                                                color = Color(0xFFE65100),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // View More Details Button
                    OutlinedButton(
                        onClick = onViewMoreDetails,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenPrimary)
                    ) {
                        Text(
                            text = when (language) { "te" -> "మరిన్ని వివరాలు చూడండి ↓"; "hi" -> "अधिक विवरण देखें ↓"; else -> "View More Details ↓" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // On-Device Llama Advisory
            AdvisoryCard(
                advisoryText = advisoryText,
                isGenerating = isGeneratingAdvice,
                language = language
            )

            // KVK Escalation Button if Severe
            if (shouldRefer) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { /* Dial KVK Helpline */ },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            "te" -> "📞 కృషి విజ్ఞాన కేంద్రం (KVK) నిపుణుడిని సంప్రదించండి"
                            "hi" -> "📞 कृषि वैज्ञानिक से संपर्क करें (KVK)"
                            else -> "📞 Contact KVK Extension Specialist"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
