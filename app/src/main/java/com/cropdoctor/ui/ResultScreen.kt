package com.cropdoctor.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.advisor.OfflineTtsHelper
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.ScanHistoryRepository
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.components.LanguageChip
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun ResultScreen(
    result: ClassifierResult,
    kbEntry: KbEntry,
    language: String,
    advisoryText: String,
    isGeneratingAdvice: Boolean,
    isFakeAdvisor: Boolean = true,
    capturedBitmap: Bitmap? = null,
    onBackClick: () -> Unit,
    onChangeLanguageClick: (String) -> Unit = {},
    onViewMoreDetails: () -> Unit,
    onChatbotClick: () -> Unit = {},
    onSaveCaseReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // openTier decision based on RulesEngine (T2)
    val openTier = RulesEngine.determineOpenTier(result.severityBand)
    var selectedTreatmentTab by remember(openTier) {
        mutableStateOf(
            if (openTier == RulesEngine.OpenTier.CHEMICAL_FIRST) "chemical" else "natural"
        )
    }

    val isUncertain = result.classId == "unknown" || result.certaintyBand == CertaintyBand.NOT_SURE
    val shouldRefer = RulesEngine.shouldShowExtensionWorker(result.certaintyBand, result.severityBand)
    val isHealthy = result.classId.endsWith("_healthy")
    var showJudgeDialog by remember { mutableStateOf(false) }
    var showMeasurementDetails by remember { mutableStateOf(false) }

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
                    Text("• Certainty Band: ${result.certaintyBand.name} (${(result.confidence * 100).toInt()}%)")
                    Text("• Severity Band: ${result.severityBand.name} (${(result.lesionFraction * 100).toInt()}% lesion)")
                    Text("• Mode: ${if (result.isDemoMode) "DEMO MODE (Fake Classifier)" else "Live Inference"}")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showJudgeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text(stringResource(R.string.close))
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
                            contentDescription = stringResource(R.string.btn_back),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.result_title),
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

                // Persistent language chip on every screen (T2)
                LanguageChip(
                    currentLanguage = language,
                    onLanguageSelected = { newLang ->
                        onChangeLanguageClick(newLang)
                    }
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
                .padding(16.dp)
        ) {
            // Demo Mode Banner
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
                            text = "⚠️ " + stringResource(R.string.demo_banner),
                            color = Color(0xFF856404),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Scanned Leaf Photo Card
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

            // Alert Banner (Disease / Healthy / Uncertain)
            val alertBannerText = when {
                isUncertain -> "⚠️ " + stringResource(R.string.cant_tell_no_disease)
                isHealthy -> "✓ " + stringResource(R.string.alert_healthy)
                else -> "🚨 " + stringResource(R.string.alert_disease_detected)
            }
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
                        stringResource(R.string.cant_tell_no_disease)
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
                        text = if (isSpeaking) stringResource(R.string.btn_stop) + " ⏹️" else stringResource(R.string.btn_listen) + " 🔊",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpeaking) Color(0xFFC62828) else FarmGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CERTAINTY PRESENTATION (T2: Icon + Colour + Word, Percentage ONLY in Details)
            val (certaintyWordRes, certaintyColor, certaintyIcon) = when (result.certaintyBand) {
                CertaintyBand.SURE -> Triple(R.string.certainty_sure, Color(0xFF2E7D32), Icons.Default.CheckCircle)
                CertaintyBand.PROBABLY -> Triple(R.string.certainty_probably, Color(0xFFD97706), Icons.Default.Info)
                CertaintyBand.NOT_SURE -> Triple(R.string.certainty_not_sure, Color(0xFFD32F2F), Icons.Default.Warning)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.certainty_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = certaintyIcon,
                                contentDescription = null,
                                tint = certaintyColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(certaintyWordRes),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = certaintyColor
                            )
                        }
                    }

                    // Expandable Measurement Details Panel (Rule: percentage ONLY in details)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showMeasurementDetails = !showMeasurementDetails },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (showMeasurementDetails) "▾ " + stringResource(R.string.btn_hide_details) else "▸ " + stringResource(R.string.btn_view_details),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FarmGreenPrimary
                        )
                    }

                    if (showMeasurementDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.confidence_score_label)}: ${(result.confidence * 100).toInt()}%",
                                fontSize = 14.sp,
                                color = Color(0xFF444444)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SEVERITY / STATUS PRESENTATION (T2b Fix: When certainty is NOT_SURE, do not show severity or early warning; show "Can't tell - no disease identified")
            if (result.certaintyBand == CertaintyBand.NOT_SURE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.cant_tell_no_disease),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            } else {
                val (severityWordRes, severityColor, severityIcon) = when (result.severityBand) {
                    SeverityBand.MILD -> Triple(R.string.severity_mild, Color(0xFF2E7D32), Icons.Default.Eco)
                    SeverityBand.MODERATE -> Triple(R.string.severity_moderate, Color(0xFFD97706), Icons.Default.WarningAmber)
                    SeverityBand.SEVERE -> Triple(R.string.severity_severe, Color(0xFFD32F2F), Icons.Default.ReportProblem)
                }
                val severityActionRes = RulesEngine.getSeverityActionSentence(result.severityBand)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.severity_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF333333)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = severityIcon,
                                    contentDescription = null,
                                    tint = severityColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(severityWordRes),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = severityColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // One-line action banner: early warning or act now (T2)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (result.severityBand == SeverityBand.MILD) Color(0xFFE8F5E9) else Color(0xFFFFF3CD),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(severityActionRes),
                                fontSize = 14.sp,
                                color = if (result.severityBand == SeverityBand.MILD) Color(0xFF1B5E20) else Color(0xFF856404),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About the Condition Section
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
                            text = stringResource(R.string.about_condition),
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

            // Treatment Suggestions Section (Tiers: Natural and Chemical)
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
                                text = stringResource(R.string.treatment_title),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab toggle row (Tier 1 Natural vs Tier 2 Chemical)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5E9), RoundedCornerShape(10.dp))
                                .padding(4.dp)
                        ) {
                            Button(
                                onClick = { selectedTreatmentTab = "natural" },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTreatmentTab == "natural") FarmGreenPrimary else Color.Transparent,
                                    contentColor = if (selectedTreatmentTab == "natural") Color.White else Color(0xFF333333)
                                ),
                                elevation = null
                            ) {
                                Text(
                                    text = "🌿 " + stringResource(R.string.tier_natural),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { selectedTreatmentTab = "chemical" },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTreatmentTab == "chemical") Color(0xFF1565C0) else Color.Transparent,
                                    contentColor = if (selectedTreatmentTab == "chemical") Color.White else Color(0xFF333333)
                                ),
                                elevation = null
                            ) {
                                Text(
                                    text = "🧪 " + stringResource(R.string.tier_chemical),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Treatment Steps
                        if (selectedTreatmentTab == "natural") {
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
                                    text = stringResource(R.string.no_chemical_required),
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
                                text = stringResource(R.string.btn_more_details) + " ↓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // On-Device Advisory Card (T2b/T2c Fix: When certainty is Not sure, no severity line or immediate step)
            val filteredAdvisoryText = if (isUncertain) {
                advisoryText.lines().filterNot { line ->
                    line.contains("Severity Guidance", ignoreCase = true) ||
                    line.contains("Immediate Step", ignoreCase = true) ||
                    line.contains("నివారణ సలహా", ignoreCase = true) ||
                    line.contains("తక్షణ చర్య", ignoreCase = true) ||
                    line.contains("सलाह", ignoreCase = true) ||
                    line.contains("तत्काल कदम", ignoreCase = true)
                }.joinToString("\n").trim()
            } else {
                advisoryText
            }

            AdvisoryCard(
                advisoryText = filteredAdvisoryText,
                isGenerating = isGeneratingAdvice,
                language = language,
                isFake = isFakeAdvisor
            )

            // ACTIVE KISAN CALL CENTRE EMERGENCY DIALER (T2: Not sure or severe -> red button)
            if (shouldRefer) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${RulesEngine.EXTENSION_OFFICER_NUMBER}"))
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
                        text = "📞 " + stringResource(R.string.kisan_call_centre_label),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // T3: Save Case Report Button (R8: under 1-minute demo flow)
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSaveCaseReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
            ) {
                Text(
                    text = "📝 " + stringResource(R.string.btn_save_case_report),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
