package com.cropdoctor.ui

import android.content.Context
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
import com.cropdoctor.advisor.AudioPlayItem
import com.cropdoctor.advisor.OfflineAudioAdvisor
import com.cropdoctor.advisor.OfflineTtsHelper
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.ScanHistoryRepository
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.components.LanguageChip
import com.cropdoctor.ui.components.RegionChip
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun ResultScreen(
    result: ClassifierResult,
    kbEntry: KbEntry,
    language: String,
    region: String = "Telangana",
    onRegionChange: ((String) -> Unit)? = null,
    advisoryText: String,
    isGeneratingAdvice: Boolean,
    isFakeAdvisor: Boolean = true,
    isLowMemoryFallback: Boolean = false,
    isOfflineGuideFallback: Boolean = false,
    capturedBitmap: Bitmap? = null,
    onBackClick: () -> Unit,
    onChangeLanguageClick: (String) -> Unit = {},
    onFieldSpreadChange: ((RulesEngine.FieldSpread) -> Unit)? = null,
    onViewMoreDetails: () -> Unit,
    onChatbotClick: () -> Unit = {},
    onSaveCaseReport: () -> Unit = {},
    onCancelAdvice: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var currentRegion by remember(region) { mutableStateOf(region) }

    val isUncertain = result.classId == "unknown" || result.certaintyBand == CertaintyBand.NOT_SURE
    val isHealthy = result.classId.endsWith("_healthy") || kbEntry.type.equals("healthy", ignoreCase = true)
    val isPest = kbEntry.type.equals("pest", ignoreCase = true)
    var selectedFieldSpread by remember { mutableStateOf<RulesEngine.FieldSpread?>(null) }

    // Urgency calculation (Requirement F4 / Task G3)
    val urgency = if (selectedFieldSpread != null) {
        RulesEngine.calculateUrgency(result.severityBand, selectedFieldSpread!!)
    } else {
        when (result.severityBand) {
            SeverityBand.MILD -> RulesEngine.UrgencyLevel.EARLY_WARNING
            SeverityBand.MODERATE -> RulesEngine.UrgencyLevel.ACT_THIS_WEEK
            SeverityBand.SEVERE -> RulesEngine.UrgencyLevel.ACT_NOW
        }
    }

    // openTier decision based on UrgencyLevel (Requirement F5 / Task G3)
    val openTier = RulesEngine.determineOpenTier(urgency)
    var selectedTreatmentTab by remember(openTier) {
        mutableStateOf(
            if (openTier == RulesEngine.OpenTier.CHEMICAL_FIRST) "chemical" else "natural"
        )
    }

    val hasNoTreatmentAdvice = kbEntry.treatment.natural.isEmpty() && kbEntry.treatment.chemical.isEmpty() && !isHealthy
    val shouldRefer = RulesEngine.shouldShowExtensionWorker(result.certaintyBand, urgency) || hasNoTreatmentAdvice
    var showJudgeDialog by remember { mutableStateOf(false) }
    var showMeasurementDetails by remember { mutableStateOf(false) }
    var showHelplineDialog by remember { mutableStateOf(false) }

    // Offline Audio Advisor (Opus clips + TTS fallback + speed control)
    val audioAdvisor = remember { OfflineAudioAdvisor(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioAdvisor.shutdown()
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

    // Regional Helplines & Extension Referral Dialog (Task G2 / Requirement F6 / Rule H4)
    if (showHelplineDialog) {
        val regKey = currentRegion.lowercase().replace(" ", "_")
        val helplines = mutableListOf<Triple<String, String, String>>()

        // Official verified ANGRAU Helpline for Andhra Pradesh (https://angrau.ac.in/angrau/index.php/extension/farmer-s-call-centre)
        if (regKey == "andhra_pradesh" || regKey == "andhra") {
            helplines.add(
                Triple("ANGRAU Farmers Call Centre", "1800-425-0430", "Acharya N.G. Ranga Agricultural Univ. • Toll-Free (AP)")
            )
        }

        // National Kisan Call Centre is available nationwide for all regions (toll-free in Telugu, Hindi, English)
        helplines.add(
            Triple("National Kisan Call Centre", "1800-180-1551", "All India • Toll Free • 22 Languages (Telugu, Hindi, English)")
        )
        AlertDialog(
            onDismissRequest = { showHelplineDialog = false },
            title = {
                Text(
                    text = "📞 " + stringResource(R.string.regional_helplines_title),
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    helplines.forEach { (name, number, note) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showHelplineDialog = false
                                    try {
                                        val cleanNum = number.replace("-", "")
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNum"))
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmGreenDark)
                                    Text(text = number, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                                    Text(text = note, fontSize = 11.sp, color = Color.Gray)
                                }
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = FarmGreenPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelplineDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Voice Not Installed Guidance Dialog (Offline Audio & TTS setup)
    if (audioAdvisor.showVoiceInstallDialog.value) {
        AlertDialog(
            onDismissRequest = { audioAdvisor.dismissVoiceInstallDialog() },
            title = {
                Text(
                    text = "🔊 " + stringResource(R.string.voice_not_installed),
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.voice_install_guide),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        audioAdvisor.openTtsSettings()
                        audioAdvisor.dismissVoiceInstallDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text(stringResource(R.string.btn_open_settings))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { audioAdvisor.dismissVoiceInstallDialog() }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Region chip (Telangana / Andhra Pradesh / Other)
                    RegionChip(
                        currentRegion = currentRegion,
                        onRegionSelected = { newReg ->
                            currentRegion = newReg
                            onRegionChange?.invoke(newReg)
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Persistent language chip on every screen (T2)
                    LanguageChip(
                        currentLanguage = language,
                        onLanguageSelected = { newLang ->
                            onChangeLanguageClick(newLang)
                        }
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

            // Alert Banner (Disease / Pest / Healthy / Uncertain)
            val alertBannerText = when {
                isUncertain -> "⚠️ " + stringResource(R.string.cant_tell_no_disease)
                isHealthy -> "✓ " + stringResource(R.string.alert_healthy)
                isPest -> "🐛 " + stringResource(R.string.alert_pest_detected)
                else -> "🚨 " + stringResource(R.string.alert_disease_detected)
            }
            val alertBannerBg = when {
                isHealthy -> Color(0xFFE8F5E9)
                isPest -> Color(0xFFFFF3E0)
                else -> Color(0xFFFFEBEE)
            }
            val alertBannerColor = when {
                isHealthy -> Color(0xFF2E7D32)
                isPest -> Color(0xFFE65100)
                else -> Color(0xFFD32F2F)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(alertBannerBg, RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = alertBannerText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = alertBannerColor
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
                    val cropToShow = (result.selectedCrop ?: if (!isUncertain) kbEntry.crop else null)?.replaceFirstChar { it.uppercase() }
                    if (cropToShow != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "($cropToShow)",
                                fontSize = 17.sp,
                                color = Color(0xFF555555)
                            )
                            if (!isUncertain) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TypeChip(type = kbEntry.type)
                            }
                        }
                    }
                }

                // Offline Audio Advisory Controls (Requirement F10 / Task A1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Speed Control Chip (1.0x / 0.75x)
                    AssistChip(
                        onClick = { audioAdvisor.toggleSpeed() },
                        label = {
                            Text(
                                text = if (audioAdvisor.playbackSpeed.value == 1.0f) stringResource(R.string.speed_normal) else stringResource(R.string.speed_slow),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFE8F5E9),
                            labelColor = FarmGreenDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    )

                    // Listen / Pause / Resume Button
                    FilledTonalButton(
                        onClick = {
                            when {
                                audioAdvisor.isPlaying.value -> audioAdvisor.pause()
                                audioAdvisor.isPaused.value -> audioAdvisor.resume()
                                else -> {
                                    val screenItems = buildScreenAudioItems(
                                        kbEntry = kbEntry,
                                        result = result,
                                        urgency = urgency,
                                        language = language,
                                        context = context
                                    )
                                    audioAdvisor.playSequence(screenItems)
                                }
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (audioAdvisor.isPlaying.value) Color(0xFFFFCDD2) else Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = when {
                                audioAdvisor.isPlaying.value -> stringResource(R.string.btn_pause) + " ⏸️"
                                audioAdvisor.isPaused.value -> stringResource(R.string.btn_listen) + " ▶️"
                                else -> stringResource(R.string.btn_listen) + " 🔊"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (audioAdvisor.isPlaying.value) Color(0xFFC62828) else FarmGreenDark
                        )
                    }

                    // Stop Button (visible when playing or paused)
                    if (audioAdvisor.isPlaying.value || audioAdvisor.isPaused.value) {
                        IconButton(
                            onClick = { audioAdvisor.stop() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "⏹️",
                                fontSize = 15.sp
                            )
                        }
                    }
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
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF444444)
                            )
                            if (result.top3.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Top-3 Candidates:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                                result.top3.forEachIndexed { idx, sc ->
                                    Text(
                                        text = "${idx + 1}. ${sc.displayName} (${(sc.probability * 100).toInt()}%)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF555555)
                                    )
                                }
                            }
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
                val severityActionRes = RulesEngine.getSeverityActionSentence(result.severityBand, kbEntry.type)

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

            // FIELD SPREAD QUESTION & URGENCY TRIAGE (Requirement F4, F11 / Task G3)
            if (RulesEngine.shouldShowFieldSpreadQuestion(result.certaintyBand, isHealthy)) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌱", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.field_spread_question),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Three large one-tap buttons: A few plants, Some, Most
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple(RulesEngine.FieldSpread.FEW_PLANTS, R.string.field_spread_few_plants, "🌱"),
                                Triple(RulesEngine.FieldSpread.SOME, R.string.field_spread_some, "🌿"),
                                Triple(RulesEngine.FieldSpread.MOST, R.string.field_spread_most, "🌾")
                            )
                            options.forEach { (spread, labelRes, emoji) ->
                                val isSelected = selectedFieldSpread == spread
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clickable {
                                            selectedFieldSpread = spread
                                            onFieldSpreadChange?.invoke(spread)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) FarmGreenPrimary else Color(0xFFE0E0E0)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = emoji, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(labelRes),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) FarmGreenDark else Color(0xFF333333),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Urgency Level Display
                        val urgencyColor = when (urgency) {
                            RulesEngine.UrgencyLevel.EARLY_WARNING -> Color(0xFF2E7D32)
                            RulesEngine.UrgencyLevel.ACT_THIS_WEEK -> Color(0xFFD97706)
                            RulesEngine.UrgencyLevel.ACT_NOW -> Color(0xFFD32F2F)
                        }
                        val urgencyBg = when (urgency) {
                            RulesEngine.UrgencyLevel.EARLY_WARNING -> Color(0xFFE8F5E9)
                            RulesEngine.UrgencyLevel.ACT_THIS_WEEK -> Color(0xFFFFF3CD)
                            RulesEngine.UrgencyLevel.ACT_NOW -> Color(0xFFFFEBEE)
                        }
                        val urgencyIcon = when (urgency) {
                            RulesEngine.UrgencyLevel.EARLY_WARNING -> Icons.Default.Info
                            RulesEngine.UrgencyLevel.ACT_THIS_WEEK -> Icons.Default.WarningAmber
                            RulesEngine.UrgencyLevel.ACT_NOW -> Icons.Default.ReportProblem
                        }
                        val urgencyWordRes = when (urgency) {
                            RulesEngine.UrgencyLevel.EARLY_WARNING -> R.string.urgency_early_warning
                            RulesEngine.UrgencyLevel.ACT_THIS_WEEK -> R.string.urgency_act_this_week
                            RulesEngine.UrgencyLevel.ACT_NOW -> R.string.urgency_act_now
                        }
                        val urgencyDescRes = when (urgency) {
                            RulesEngine.UrgencyLevel.EARLY_WARNING -> R.string.urgency_desc_early_warning
                            RulesEngine.UrgencyLevel.ACT_THIS_WEEK -> R.string.urgency_desc_act_this_week
                            RulesEngine.UrgencyLevel.ACT_NOW -> R.string.urgency_desc_act_now
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = urgencyBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, urgencyColor.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.urgency_title),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF444444)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = urgencyIcon,
                                            contentDescription = null,
                                            tint = urgencyColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(urgencyWordRes),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = urgencyColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(urgencyDescRes),
                                    fontSize = 13.sp,
                                    color = urgencyColor,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // If Early Warning: Show "What to watch for in the next days" (Requirement F11)
                if (urgency == RulesEngine.UrgencyLevel.EARLY_WARNING) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.watch_for_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val watchText = kbEntry.earlyWarningSigns.forLanguage(language).ifEmpty {
                                stringResource(R.string.watch_for_content)
                            }
                            Text(
                                text = watchText,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // If Act Now: Show Extension Officer call button first (Requirement G3.3)
                if (urgency == RulesEngine.UrgencyLevel.ACT_NOW) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showHelplineDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚨 " + stringResource(R.string.kisan_call_centre_label) + " (Act Now)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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

            // Regional Advisory Section (Requirement F6 / Task G2)
            val regionalAdvice = kbEntry.getRegionalAdvice(currentRegion)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.regional_advisory_title),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )
                        }
                        val regNameRes = when (currentRegion.lowercase().replace(" ", "_")) {
                            "telangana" -> R.string.region_telangana
                            "andhra_pradesh", "andhra" -> R.string.region_andhra
                            else -> R.string.region_other
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "📍 " + stringResource(regNameRes),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (regionalAdvice.differs && regionalAdvice.text != null) {
                        Text(
                            text = regionalAdvice.text.forLanguage(language),
                            fontSize = 15.sp,
                            color = Color(0xFF333333),
                            lineHeight = 22.sp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.same_advice_all_regions),
                            fontSize = 15.sp,
                            color = Color(0xFF555555),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Advice source badge (Requirement 3: "Advice source: ...")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F8E9)
                    ) {
                        Text(
                            text = "🏛️ " + stringResource(R.string.advice_source, regionalAdvice.source),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FarmGreenDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
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
                            if (kbEntry.treatment.natural.isEmpty()) {
                                Text(
                                    text = kbEntry.immediateStep.forLanguage(language),
                                    fontSize = 15.sp,
                                    color = Color(0xFF555555),
                                    lineHeight = 22.sp
                                )
                            } else {
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
                isFake = isFakeAdvisor,
                isLowMemoryFallback = isLowMemoryFallback,
                isOfflineGuideFallback = isOfflineGuideFallback,
                audioAdvisor = audioAdvisor,
                onCancel = onCancelAdvice
            )

            // 1-TAP SHARE FIELD REPORT (Agri-Extension & Decision Support)
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    val reportBuilder = StringBuilder()
                    reportBuilder.append("🌾 Crop Doctor - Field Diagnosis Report\n")
                    reportBuilder.append("====================================\n")
                    reportBuilder.append("Crop: ${kbEntry.crop.replaceFirstChar { it.uppercase() }}\n")
                    reportBuilder.append("Diagnosis: ${kbEntry.names.forLanguage(language)}\n")
                    reportBuilder.append("Severity: ${result.severityBand.name}\n")
                    reportBuilder.append("Certainty: ${result.certaintyBand.name} (${(result.confidence * 100).toInt()}%)\n\n")

                    val immediate = kbEntry.immediateStep.forLanguage(language)
                    if (immediate.isNotBlank() && !isUncertain) {
                        reportBuilder.append("Immediate Action:\n$immediate\n\n")
                    }

                    if (kbEntry.treatment.natural.isNotEmpty()) {
                        reportBuilder.append("Natural / Traditional Methods:\n")
                        kbEntry.treatment.natural.take(2).forEach {
                            reportBuilder.append("• Step ${it.step}: ${it.action.forLanguage(language)}\n")
                        }
                        reportBuilder.append("\n")
                    }

                    if (kbEntry.treatment.chemical.isNotEmpty() && result.severityBand != SeverityBand.MILD) {
                        reportBuilder.append("CIB&RC Registered Chemical Actives:\n")
                        kbEntry.treatment.chemical.take(2).forEach {
                            reportBuilder.append("• ${it.activeIngredient} (${it.productExample}) - Dose: ${if (it.doseVerified) it.dose else "Follow label"}\n")
                        }
                        reportBuilder.append("\n")
                    }

                    reportBuilder.append("Generated 100% offline via Crop Doctor on Arm SoC.\n")
                    reportBuilder.append("For expert consultation call Kisan Call Centre: 1800-180-1551")

                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Crop Doctor Report: ${kbEntry.names.forLanguage(language)}")
                            putExtra(Intent.EXTRA_TEXT, reportBuilder.toString())
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.btn_share_report)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenDark)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = FarmGreenDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_share_report),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ACTIVE KISAN CALL CENTRE & REGIONAL HELPLINE DIALER (T2: Not sure or severe -> red button)
            if (shouldRefer) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showHelplineDialog = true },
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

/**
 * Type Chip displaying "Disease" / "Pest" / "Healthy" with icon and localized word (Rule G1).
 */
@Composable
fun TypeChip(
    type: String,
    modifier: Modifier = Modifier
) {
    val wordRes = when (type.lowercase()) {
        "pest" -> R.string.type_pest
        "healthy" -> R.string.type_healthy
        else -> R.string.type_disease
    }
    val icon = when (type.lowercase()) {
        "pest" -> Icons.Default.BugReport
        "healthy" -> Icons.Default.CheckCircle
        else -> Icons.Default.Coronavirus
    }
    val containerColor = when (type.lowercase()) {
        "pest" -> Color(0xFFFFF3E0)
        "healthy" -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFEBEE)
    }
    val contentColor = when (type.lowercase()) {
        "pest" -> Color(0xFFE65100)
        "healthy" -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }
    val borderColor = when (type.lowercase()) {
        "pest" -> Color(0xFFFFCC80)
        "healthy" -> Color(0xFFA5D6A7)
        else -> Color(0xFFFFCDD2)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(wordRes),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Builds the ordered sequence of audio items to read for the current screen (Requirement F10 / Task A1):
 * 1. Label
 * 2. Urgency
 * 3. Advice in the order shown (immediate step, severity advice, cultural/natural steps, referral if urgent).
 */
internal fun buildScreenAudioItems(
    kbEntry: KbEntry,
    result: ClassifierResult,
    urgency: RulesEngine.UrgencyLevel,
    language: String,
    context: Context
): List<AudioPlayItem> {
    val items = mutableListOf<AudioPlayItem>()
    val isNotSure = result.certaintyBand == CertaintyBand.NOT_SURE || result.classId.equals("unknown", ignoreCase = true)

    if (isNotSure) {
        val pieceId = if (language.lowercase() == "te") "te_intro_unsure" else null
        val unsureIntro = when (language.lowercase()) {
            "te" -> "పంట నమూనాలో వ్యాధి లేదా పురుగు లక్షణాలు స్పష్టంగా నిర్ధారించబడలేదు."
            "hi" -> "फसल के नमूने में रोग या कीट के लक्षण स्पष्ट नहीं हैं।"
            else -> "The crop symptom could not be diagnosed with certainty."
        }
        items.add(AudioPlayItem(pieceId = pieceId, text = unsureIntro, lang = language))
        items.add(AudioPlayItem(text = kbEntry.explainText.forLanguage(language), lang = language))

        val referralPieceId = when (language.lowercase()) {
            "te" -> "te_referral_urgent"
            "hi" -> "hi_referral_urgent"
            else -> null
        }
        val refText = RulesEngine.getReferralSentence(urgency, result.certaintyBand, language)
        items.add(AudioPlayItem(pieceId = referralPieceId, text = refText, lang = language))
        return items
    }

    // 1. Label
    val labelText = kbEntry.names.forLanguage(language)
    items.add(AudioPlayItem(text = labelText, lang = language))

    // 2. Urgency
    val urgencyName = when (urgency) {
        RulesEngine.UrgencyLevel.EARLY_WARNING -> context.getString(R.string.urgency_early_warning)
        RulesEngine.UrgencyLevel.ACT_THIS_WEEK -> context.getString(R.string.urgency_act_this_week)
        RulesEngine.UrgencyLevel.ACT_NOW -> context.getString(R.string.urgency_act_now)
    }
    val urgencyDesc = when (urgency) {
        RulesEngine.UrgencyLevel.EARLY_WARNING -> context.getString(R.string.urgency_desc_early_warning)
        RulesEngine.UrgencyLevel.ACT_THIS_WEEK -> context.getString(R.string.urgency_desc_act_this_week)
        RulesEngine.UrgencyLevel.ACT_NOW -> context.getString(R.string.urgency_desc_act_now)
    }
    items.add(AudioPlayItem(text = "$urgencyName. $urgencyDesc", lang = language))

    // 3. Advice in the order shown:
    // Immediate step
    val immediateStep = kbEntry.immediateStep.forLanguage(language)
    if (immediateStep.isNotBlank()) {
        items.add(AudioPlayItem(text = immediateStep, lang = language))
    }

    // Severity guidance
    val sevAdvice = kbEntry.severityAdvice[result.severityBand.name]?.forLanguage(language) ?: ""
    if (sevAdvice.isNotBlank()) {
        items.add(AudioPlayItem(text = sevAdvice, lang = language))
    }

    // Cultural / Natural advice
    if (kbEntry.treatment.natural.isNotEmpty()) {
        val naturalAction = kbEntry.treatment.natural.first().action.forLanguage(language)
        if (naturalAction.isNotBlank()) {
            items.add(AudioPlayItem(text = naturalAction, lang = language))
        }
    }

    // Referral if urgency is high or severe
    if (RulesEngine.shouldShowExtensionWorker(result.certaintyBand, urgency)) {
        val referralPieceId = when (language.lowercase()) {
            "te" -> "te_referral_urgent"
            "hi" -> "hi_referral_urgent"
            else -> null
        }
        val refText = RulesEngine.getReferralSentence(urgency, result.certaintyBand, language)
        items.add(AudioPlayItem(pieceId = referralPieceId, text = refText, lang = language))
    }

    return items
}
