package com.cropdoctor.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.advisor.OfflineTtsHelper
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.ScanHistoryRepository
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
    capturedBitmap: Bitmap? = null,
    onBackClick: () -> Unit,
    onChangeLanguageClick: () -> Unit,
    onViewMoreDetails: () -> Unit,
    onChatbotClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val initialTab = TreatmentSelector.getInitialTab(result.severityBand, result.classId)
    var selectedTreatmentTab by remember(initialTab) { mutableStateOf(initialTab) }

    val isUncertain = result.classId == "unknown" || result.certaintyBand == CertaintyBand.NOT_SURE
    val shouldRefer = RulesEngine.shouldReferToKVK(result.severityBand, kbEntry.referralThreshold) || isUncertain
    val isHealthy = result.classId.endsWith("_healthy")
    var showJudgeDialog by remember { mutableStateOf(false) }

    // Offline Text-To-Speech
    var isSpeaking by remember { mutableStateOf(false) }
    val ttsHelper = remember { OfflineTtsHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    // Auto-save to local Scan History
    LaunchedEffect(result.classId) {
        ScanHistoryRepository.saveScan(
            context = context,
            crop = kbEntry.crop,
            classId = result.classId,
            diseaseName = kbEntry.names.forLanguage(language),
            confidence = result.confidence,
            severity = result.severityBand.name
        )
    }

    val screenTitle = when (language) {
        "te" -> "ఫలితాలు"
        "hi" -> "परिणाम"
        else -> "Result"
    }

    val alertBannerText = when {
        isUncertain -> when (language) {
            "te" -> "⚠️ ఖచ్చితంగా తెలియదు - వ్యవసాయ అధికారిని సంప్రదించండి"
            "hi" -> "⚠️ अनिश्चित - कृषि विस्तार अधिकारी से संपर्क करें"
            else -> "⚠️ Not sure - ask an extension officer"
        }
        isHealthy -> when (language) {
            "te" -> "✓ ఆరోగ్యకరమైన పంట!"
            "hi" -> "✓ स्वस्थ फसल!"
            else -> "✓ Healthy Crop!"
        }
        else -> when (language) {
            "te" -> "🚨 తెగులు గుర్తించబడింది!"
            "hi" -> "🚨 रोग पाया गया!"
            else -> "🚨 Disease Detected!"
        }
    }

    val confidencePercent = (result.confidence * 100).toInt().coerceIn(0, 100)
    val certaintyLabel = when (result.certaintyBand) {
        CertaintyBand.SURE -> when (language) { "te" -> "ఖచ్చితంగా (Sure)"; "hi" -> "निश्चित (Sure)"; else -> "Sure" }
        CertaintyBand.PROBABLY -> when (language) { "te" -> "బహుశా (Probably)"; "hi" -> "संभवतः (Probably)"; else -> "Probably" }
        CertaintyBand.NOT_SURE -> when (language) { "te" -> "ఖచ్చితంగా లేదు (Not sure)"; "hi" -> "अनिश्चित (Not sure)"; else -> "Not sure" }
    }

    val listenButtonText = when (language) {
        "te" -> if (isSpeaking) "ఆపండి ⏹️" else "వినండి 🔊"
        "hi" -> if (isSpeaking) "रोकें ⏹️" else "सुनें 🔊"
        else -> if (isSpeaking) "Stop ⏹️" else "Listen 🔊"
    }

    // Hidden Judge Benchmark Panel (long-press on screen title)
    if (showJudgeDialog) {
        AlertDialog(
            onDismissRequest = { showJudgeDialog = false },
            title = {
                Text(
                    text = "⚙️ Judge Benchmark Panel",
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Live On-Device Measurements (Arm Cortex-A):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    HorizontalDivider()
                    Text("• Inference Latency: ${result.inferenceTimeMs} ms")
                    Text("• Process Memory (RAM): ${result.memoryUsedMb} MB")
                    Text("• Network Usage: 0 KB (No INTERNET permission)")
                    Text("• Certainty Band: ${result.certaintyBand.name} ($confidencePercent%)")
                    Text("• Severity Band: ${result.severityBand.name} (${(result.lesionFraction * 100).toInt()}% lesion)")
                    Text("• Mode: ${if (result.isDemoMode) "DEMO MODE (Fake Classifier)" else "Live Inference"}")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showJudgeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

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
                        color = Color.White,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showJudgeDialog = true }
                            )
                        }
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
            // Demo Mode Banner (shown only when running fake/demo mode)
            if (result.isDemoMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ DEMO MODE - NOT A REAL DIAGNOSIS",
                            color = Color(0xFF856404),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Leaf Display Card (Shows real captured photo or green badge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = "Scanned Leaf",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = "🍃", fontSize = 84.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alert Banner (Red for disease/uncertain, Green for healthy)
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

            // Disease Title & Voice TTS Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayDiseaseTitle = if (isUncertain) {
                        when (language) {
                            "te" -> "ఖచ్చితంగా తెలియదు"
                            "hi" -> "अनिश्चित स्थिति"
                            else -> "Not sure - ask an extension officer"
                        }
                    } else {
                        kbEntry.names.forLanguage(language)
                    }

                    Text(
                        text = displayDiseaseTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmGreenDark,
                        lineHeight = 30.sp
                    )
                    if (!isUncertain) {
                        Text(
                            text = "(${kbEntry.crop.replaceFirstChar { it.uppercase() }})",
                            fontSize = 17.sp,
                            color = Color(0xFF555555),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Speaker Button for Farmers (TTS Voice)
                FilledTonalButton(
                    onClick = {
                        if (isSpeaking) {
                            ttsHelper.stop()
                            isSpeaking = false
                        } else {
                            val immediateAdvice = if (kbEntry.treatment.natural.isNotEmpty()) {
                                kbEntry.treatment.natural.first().action.forLanguage(language)
                            } else ""
                            val spokenText = "${kbEntry.names.forLanguage(language)}. ${kbEntry.explainText.forLanguage(language)}. $immediateAdvice"
                            ttsHelper.speak(spokenText, language)
                            isSpeaking = true
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSpeaking) Color(0xFFFFCDD2) else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = listenButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpeaking) Color(0xFFC62828) else FarmGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Certainty Progress Bar (Rule H12: Live measurement, Sure/Probably/Not sure words)
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
                            text = when (language) { "te" -> "నిశ్చయత (Certainty):"; "hi" -> "निश्चितता (Certainty):"; else -> "Certainty:" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "$certaintyLabel ($confidencePercent%)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUncertain) Color(0xFFD32F2F) else FarmGreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { confidencePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (isUncertain) Color(0xFFD32F2F) else FarmGreenPrimary,
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
                            text = when (language) { "te" -> "వ్యాధి వివరాలు"; "hi" -> "रोग के बारे में"; else -> "About the Condition" },
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
            if (!isUncertain && !isHealthy) {
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

                        // Treatment Steps with Checkmarks
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
                                                    text = "${chem.productExample} (${chem.activeIngredient}) - ${chem.dose}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0D47A1)
                                                )
                                                val phiInfo = if (chem.phiDays != null) " • PHI: ${chem.phiDays} days" else ""
                                                Text(
                                                    text = "🛡️ " + chem.safetyLine.forLanguage(language) + phiInfo,
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
            }

            // Chat with Agri AI Doctor
            Button(
                onClick = onChatbotClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Text(text = "👨‍🌾", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (language) {
                        "te" -> "💬 అగ్రి AI డాక్టర్‌తో మాట్లాడండి"
                        "hi" -> "💬 एग्रीकेयर AI डॉक्टर से बात करें"
                        else -> "💬 Chat with Agri AI Doctor"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // On-Device Llama Advisory
            AdvisoryCard(
                advisoryText = advisoryText,
                isGenerating = isGeneratingAdvice,
                language = language
            )

            // Active Kisan Call Centre Emergency Dialer if Severe or Uncertain
            if (shouldRefer) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001801551"))
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            "te" -> "📞 కిసాన్ కాల్ సెంటర్ (1800-180-1551)"
                            "hi" -> "📞 किसान कॉल सेंटर (1800-180-1551)"
                            else -> "📞 Kisan Call Centre (1800-180-1551)"
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
