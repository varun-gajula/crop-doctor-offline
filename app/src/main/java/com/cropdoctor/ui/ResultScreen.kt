package com.cropdoctor.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.CropDoctorApplication
import com.cropdoctor.R
import com.cropdoctor.advisor.Advisor
import com.cropdoctor.advisor.AudioPlayItem
import com.cropdoctor.advisor.OfflineAudioAdvisor
import com.cropdoctor.advisor.OfflineTtsHelper
import com.cropdoctor.chat.PassageStore
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.CropMapping
import com.cropdoctor.domain.DiagnosisOutcome
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
    advisorSource: String = "OFFLINE_GUIDE",
    capturedBitmap: Bitmap? = null,
    onBackClick: () -> Unit,
    onChangeLanguageClick: (String) -> Unit = {},
    onFieldSpreadChange: ((RulesEngine.FieldSpread) -> Unit)? = null,
    onViewMoreDetails: () -> Unit,
    onChatbotClick: () -> Unit = {},
    onSaveCaseReport: () -> Unit = {},
    onCancelAdvice: () -> Unit = {},
    onChangeCropClick: () -> Unit = {},
    onTakeAnotherPhoto: () -> Unit = {},
    onInstallModelClick: () -> Unit = {},
    advisor: Advisor? = null,
    passageStore: PassageStore? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var currentRegion by remember(region) { mutableStateOf(region) }

    val cropDisplayName = CropMapping.getCropDisplayName(result.selectedCrop ?: kbEntry.crop, context)
    val isUncertain = result.outcome != DiagnosisOutcome.DIAGNOSE || result.classId == "unknown" || result.certaintyBand == CertaintyBand.NOT_SURE
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

    val openTier = RulesEngine.determineOpenTier(urgency)
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

    // Auto-save to local Scan History (only for confident DIAGNOSE decisions)
    LaunchedEffect(result.classId, result.outcome) {
        if (result.outcome == DiagnosisOutcome.DIAGNOSE) {
            ScanHistoryRepository.saveScan(
                context = context,
                crop = kbEntry.crop,
                classId = result.classId,
                diseaseName = kbEntry.names.forLanguage(language),
                confidence = result.confidence,
                severity = result.severityBand.name
            )
        }
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
                    Text("• Outcome: ${result.outcome.name}")
                    Text("• Outcome Reason: ${result.outcomeReason}")
                    Text("• Free Energy: ${"%.2f".format(result.freeEnergy)}")
                    Text("• Crop Mass: ${"%.2f".format(result.cropMass)}")
                    Text("• Margin: ${"%.2f".format(result.margin)}")
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

    // Regional Helplines & Extension Referral Dialog
    if (showHelplineDialog) {
        val regKey = currentRegion.lowercase().replace(" ", "_")
        val helplines = mutableListOf<Triple<String, String, String>>()

        if (regKey == "andhra_pradesh" || regKey == "andhra") {
            helplines.add(
                Triple("ANGRAU Farmers Call Centre", "1800-425-0430", "Acharya N.G. Ranga Agricultural Univ. • Toll-Free (AP)")
            )
        }
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
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FarmGreenDark)
                                    Text(text = number, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                                    Text(text = note, fontSize = 13.sp, color = Color(0xFF555555))
                                }
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = FarmGreenPrimary,
                                    modifier = Modifier.size(28.dp)
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

    // Voice Not Installed Guidance Dialog
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
                    fontSize = 16.sp,
                    lineHeight = 24.sp
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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    val topTitleRes = when (result.outcome) {
                        DiagnosisOutcome.CANT_TELL -> R.string.outcome_cant_tell_title
                        DiagnosisOutcome.OUT_OF_CLASS -> R.string.outcome_out_of_class_title
                        DiagnosisOutcome.DIAGNOSE -> R.string.result_title
                    }
                    Text(
                        text = stringResource(topTitleRes),
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
                    RegionChip(
                        currentRegion = currentRegion,
                        onRegionSelected = { newReg ->
                            currentRegion = newReg
                            onRegionChange?.invoke(newReg)
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    LanguageChip(
                        currentLanguage = language,
                        onLanguageSelected = { newLang ->
                            onChangeLanguageClick(newLang)
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            // Requirement 3: Floating "Listen" button for offline voice guidance
            if (result.outcome == DiagnosisOutcome.DIAGNOSE) {
                ExtendedFloatingActionButton(
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
                    icon = {
                        Icon(
                            imageVector = when {
                                audioAdvisor.isPlaying.value -> Icons.Default.Pause
                                audioAdvisor.isPaused.value -> Icons.Default.PlayArrow
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = "Listen",
                            tint = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = when {
                                audioAdvisor.isPlaying.value -> stringResource(R.string.btn_pause)
                                audioAdvisor.isPaused.value -> stringResource(R.string.btn_resume)
                                else -> stringResource(R.string.btn_listen)
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    containerColor = if (audioAdvisor.isPlaying.value) Color(0xFFC62828) else FarmGreenPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.heightIn(min = 52.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F8F4))
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Demo Mode Banner (if active)
            if (result.isDemoMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFEEBA))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ " + stringResource(R.string.demo_banner),
                            color = Color(0xFF856404),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Scanned Leaf Photo Card (White card, 16dp rounded corners, soft shadow)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
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
                        Text(text = "🍃", fontSize = 80.sp)
                    }
                }
            }

            // Requirement 3: Crop and problem name large at top
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Crop Badge / Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "🌱 $cropDisplayName",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (result.outcome == DiagnosisOutcome.DIAGNOSE) {
                            TypeChip(type = kbEntry.type)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Large Problem Name
                    val displayDiseaseTitle = when (result.outcome) {
                        DiagnosisOutcome.CANT_TELL -> stringResource(R.string.outcome_cant_tell_title)
                        DiagnosisOutcome.OUT_OF_CLASS -> stringResource(R.string.outcome_out_of_class_title)
                        DiagnosisOutcome.DIAGNOSE -> kbEntry.names.forLanguage(language)
                    }

                    Text(
                        text = displayDiseaseTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmGreenDark,
                        lineHeight = 32.sp
                    )

                    // Audio speed chip & playback state indicator (if DIAGNOSE)
                    if (result.outcome == DiagnosisOutcome.DIAGNOSE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { audioAdvisor.toggleSpeed() },
                                label = {
                                    Text(
                                        text = if (audioAdvisor.playbackSpeed.value == 1.0f) {
                                            "⚡ " + stringResource(R.string.speed_normal)
                                        } else {
                                            "🐢 " + stringResource(R.string.speed_slow)
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFF1F8E9),
                                    labelColor = FarmGreenDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            )

                            if (audioAdvisor.isPlaying.value || audioAdvisor.isPaused.value) {
                                OutlinedButton(
                                    onClick = { audioAdvisor.stop() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("⏹️ " + stringResource(R.string.btn_stop), fontSize = 12.sp, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }

            // Requirement 3: Coloured certainty badge and coloured severity badge side-by-side
            val (certaintyWordRes, certaintyColor, certaintyBg, certaintyIcon) = if (result.outcome == DiagnosisOutcome.DIAGNOSE) {
                when (result.certaintyBand) {
                    CertaintyBand.SURE -> Quadruple(R.string.certainty_sure, Color(0xFF2E7D32), Color(0xFFE8F5E9), Icons.Default.CheckCircle)
                    CertaintyBand.PROBABLY -> Quadruple(R.string.certainty_probably, Color(0xFFD97706), Color(0xFFFFF3E0), Icons.Default.Info)
                    CertaintyBand.NOT_SURE -> Quadruple(R.string.certainty_not_sure, Color(0xFFD32F2F), Color(0xFFFFEBEE), Icons.Default.Warning)
                }
            } else {
                Quadruple(R.string.certainty_not_sure, Color(0xFFD32F2F), Color(0xFFFFEBEE), Icons.Default.Warning)
            }

            val (severityWordRes, severityColor, severityBg, severityIcon) = if (result.outcome == DiagnosisOutcome.DIAGNOSE) {
                when (result.severityBand) {
                    SeverityBand.MILD -> Quadruple(R.string.severity_mild, Color(0xFF2E7D32), Color(0xFFE8F5E9), Icons.Default.Eco)
                    SeverityBand.MODERATE -> Quadruple(R.string.severity_moderate, Color(0xFFD97706), Color(0xFFFFF3E0), Icons.Default.WarningAmber)
                    SeverityBand.SEVERE -> Quadruple(R.string.severity_severe, Color(0xFFD32F2F), Color(0xFFFFEBEE), Icons.Default.ReportProblem)
                }
            } else {
                Quadruple(R.string.severity_mild, Color(0xFF757575), Color(0xFFF5F5F5), Icons.Default.Info)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Certainty Badge Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showMeasurementDetails = !showMeasurementDetails },
                    shape = RoundedCornerShape(16.dp),
                    color = certaintyBg,
                    border = BorderStroke(1.5.dp, certaintyColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.certainty_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF555555)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = certaintyIcon,
                                contentDescription = null,
                                tint = certaintyColor,
                                modifier = Modifier.size(18.dp)
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
                }

                // Severity Badge Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = severityBg,
                    border = BorderStroke(1.5.dp, severityColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.severity_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF555555)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = severityIcon,
                                contentDescription = null,
                                tint = severityColor,
                                modifier = Modifier.size(18.dp)
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
                }
            }

            // Expandable Measurement Details Panel (Rule H12: percentage ONLY in details)
            if (showMeasurementDetails) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊 " + stringResource(R.string.btn_view_details),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            TextButton(
                                onClick = { showMeasurementDetails = false },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.btn_hide_details), fontSize = 13.sp, color = FarmGreenPrimary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        if (result.outcome == DiagnosisOutcome.DIAGNOSE) {
                            Text(
                                text = "${stringResource(R.string.confidence_score_label)}: ${(result.confidence * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                            Text(text = "Severity fraction: ${(result.lesionFraction * 100).toInt()}% leaf area", fontSize = 14.sp, color = Color(0xFF444444))
                            if (result.top3.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Top-3 Candidates:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                                result.top3.forEachIndexed { idx, sc ->
                                    Text(
                                        text = "${idx + 1}. ${sc.displayName} (${(sc.probability * 100).toInt()}%)",
                                        fontSize = 13.sp,
                                        color = Color(0xFF555555)
                                    )
                                }
                            }
                        } else {
                            Text(text = "Outcome: ${result.outcome.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                            Text(text = "Reason: ${result.outcomeReason}", fontSize = 13.sp, color = Color(0xFF555555))
                            Text(text = "Selected Crop: $cropDisplayName", fontSize = 13.sp, color = Color(0xFF555555))
                            if (result.cropMass > 0f) {
                                Text(text = "Crop Mass: ${"%.2f".format(result.cropMass)}", fontSize = 13.sp, color = Color(0xFF555555))
                            }
                            Text(text = "Free Energy: ${"%.2f".format(result.freeEnergy)}", fontSize = 13.sp, color = Color(0xFF555555))
                            Text(text = "Confidence: ${(result.confidence * 100).toInt()}%", fontSize = 13.sp, color = Color(0xFF555555))
                        }
                    }
                }
            }

            // OUTCOME SPECIFIC CONTENT
            when (result.outcome) {
                DiagnosisOutcome.CANT_TELL -> {
                    // Cant tell info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFFFCDD2)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.outcome_cant_tell_title),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val cantTellText = if (result.outcomeReason.startsWith("TIMEOUT") || result.outcomeReason.startsWith("READ_ERROR") || result.outcomeReason.startsWith("ERROR")) {
                                stringResource(R.string.error_could_not_read_photo)
                            } else {
                                stringResource(R.string.outcome_cant_tell_message)
                            }
                            Text(
                                text = cantTellText,
                                fontSize = 18.sp,
                                color = Color(0xFF2C3E2D),
                                lineHeight = 26.sp
                            )
                        }
                    }

                    // Action buttons for CANT_TELL
                    Button(
                        onClick = onTakeAnotherPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_take_another_photo),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Requirement 3: Clear "Call extension officer" button for uncertain cases
                    Button(
                        onClick = { showHelplineDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_call_officer),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Task CHAT: Offline chat disabled for CANT_TELL
                    val app = context.applicationContext as? CropDoctorApplication
                    val effectiveAdvisor = advisor ?: app?.getAdvisorIfInitialized()
                    val effectivePassageStore = passageStore ?: app?.getPassageStoreIfInitialized()
                    if (effectiveAdvisor != null && effectivePassageStore != null) {
                        ResultChatSection(
                            classId = result.classId,
                            crop = kbEntry.crop,
                            problemName = cropDisplayName,
                            severity = result.severityBand.name,
                            certainty = result.certaintyBand.name,
                            language = language,
                            passageStore = effectivePassageStore,
                            advisor = effectiveAdvisor,
                            isUncertainOrOutOfClass = true,
                            onCallOfficer = { showHelplineDialog = true }
                        )
                    }
                }

                DiagnosisOutcome.OUT_OF_CLASS -> {
                    // Out of class info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFFFE082)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.outcome_out_of_class_title),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.outcome_out_of_class_message, cropDisplayName),
                                fontSize = 18.sp,
                                color = Color(0xFF2C3E2D),
                                lineHeight = 26.sp
                            )
                        }
                    }

                    // Action buttons for OUT_OF_CLASS
                    Button(
                        onClick = onChangeCropClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_change_crop),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onTakeAnotherPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, FarmGreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenDark)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = FarmGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_take_another_photo),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                    }

                    // Requirement 3: Clear "Call extension officer" button
                    Button(
                        onClick = { showHelplineDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_call_officer),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Task CHAT: Offline chat disabled for CANT_TELL
                    val app = context.applicationContext as? CropDoctorApplication
                    val effectiveAdvisor = advisor ?: app?.getAdvisorIfInitialized()
                    val effectivePassageStore = passageStore ?: app?.getPassageStoreIfInitialized()
                    if (effectiveAdvisor != null && effectivePassageStore != null) {
                        ResultChatSection(
                            classId = result.classId,
                            crop = kbEntry.crop,
                            problemName = cropDisplayName,
                            severity = result.severityBand.name,
                            certainty = result.certaintyBand.name,
                            language = language,
                            passageStore = effectivePassageStore,
                            advisor = effectiveAdvisor,
                            isUncertainOrOutOfClass = true,
                            onCallOfficer = { showHelplineDialog = true }
                        )
                    }
                }

                DiagnosisOutcome.DIAGNOSE -> {
                    // Requirement 3: Five sections as cards, collapsed by default except "What to do now"

                    // Card 1: What it is (collapsed by default)
                    ResultSectionCard(
                        title = stringResource(R.string.section_what_it_is),
                        icon = Icons.Default.Info,
                        iconTint = FarmGreenPrimary,
                        initiallyExpanded = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = kbEntry.explainText.forLanguage(language),
                                fontSize = 18.sp,
                                color = Color(0xFF2C3E2D),
                                lineHeight = 26.sp
                            )

                            if (kbEntry.pathogen.isNotBlank() && kbEntry.pathogen != "Awaiting expert verification") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F8E9)
                                ) {
                                    Text(
                                        text = "🔬 Pathogen: ${kbEntry.pathogen}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FarmGreenDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Regional advice source attribution (PJTSAU / ANGRAU / ICAR)
                            val regionalAdvice = kbEntry.getRegionalAdvice(currentRegion)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F8E9)
                            ) {
                                Text(
                                    text = "🏛️ " + stringResource(R.string.advice_source, regionalAdvice.source),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FarmGreenDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Card 2: What to do now (EXPANDED BY DEFAULT)
                    ResultSectionCard(
                        title = stringResource(R.string.section_what_to_do_now),
                        icon = Icons.Default.Bolt,
                        iconTint = Color(0xFFD97706),
                        initiallyExpanded = true
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Immediate step highlight
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = FarmGreenPrimary,
                                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = kbEntry.immediateStep.forLanguage(language),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FarmGreenDark,
                                        lineHeight = 26.sp
                                    )
                                }
                            }

                            // Field Spread Question (if applicable)
                            if (RulesEngine.shouldShowFieldSpreadQuestion(result.certaintyBand, isHealthy)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.field_spread_question),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1C271E)
                                    )
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
                                                    .heightIn(min = 60.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        selectedFieldSpread = spread
                                                        onFieldSpreadChange?.invoke(spread)
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) FarmGreenPrimary else Color(0xFFE0E0E0)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(text = emoji, fontSize = 18.sp)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = stringResource(labelRes),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) FarmGreenDark else Color(0xFF333333)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Urgency Banner
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
                                shape = RoundedCornerShape(12.dp),
                                color = urgencyBg,
                                border = BorderStroke(1.dp, urgencyColor.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = urgencyIcon,
                                            contentDescription = null,
                                            tint = urgencyColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(urgencyWordRes),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = urgencyColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(urgencyDescRes),
                                        fontSize = 15.sp,
                                        color = urgencyColor,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    // Card 3: Prevention (collapsed by default)
                    ResultSectionCard(
                        title = stringResource(R.string.section_prevention),
                        icon = Icons.Default.Shield,
                        iconTint = FarmGreenPrimary,
                        initiallyExpanded = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Watch for signs
                            val watchText = kbEntry.earlyWarningSigns.forLanguage(language).ifEmpty {
                                stringResource(R.string.watch_for_content)
                            }
                            Text(
                                text = "👀 " + stringResource(R.string.watch_for_title),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Text(
                                text = watchText,
                                fontSize = 18.sp,
                                color = Color(0xFF2C3E2D),
                                lineHeight = 26.sp
                            )

                            // Regional advice (if any)
                            val regAdvice = kbEntry.getRegionalAdvice(currentRegion)
                            if (regAdvice.differs && regAdvice.text != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📍 " + stringResource(R.string.regional_advisory_title),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                                Text(
                                    text = regAdvice.text.forLanguage(language),
                                    fontSize = 17.sp,
                                    color = Color(0xFF333333),
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }

                    // Card 4: Natural & Traditional (collapsed by default)
                    ResultSectionCard(
                        title = stringResource(R.string.section_natural),
                        icon = Icons.Default.Eco,
                        iconTint = FarmGreenPrimary,
                        initiallyExpanded = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (isHealthy) {
                                Text(
                                    text = stringResource(R.string.no_treatment_needed_healthy),
                                    fontSize = 18.sp,
                                    color = Color(0xFF2E7D32),
                                    lineHeight = 26.sp
                                )
                            } else if (kbEntry.treatment.natural.isEmpty()) {
                                Text(
                                    text = kbEntry.immediateStep.forLanguage(language),
                                    fontSize = 18.sp,
                                    color = Color(0xFF2C3E2D),
                                    lineHeight = 26.sp
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
                                            modifier = Modifier.size(22.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = step.action.forLanguage(language),
                                            fontSize = 18.sp,
                                            color = Color(0xFF2C3E2D),
                                            lineHeight = 26.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 5: Chemical (Advanced) (collapsed by default)
                    ResultSectionCard(
                        title = stringResource(R.string.section_chemical),
                        icon = Icons.Default.Science,
                        iconTint = Color(0xFF1565C0),
                        initiallyExpanded = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (isHealthy || kbEntry.treatment.chemical.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.no_chemical_advice),
                                    fontSize = 18.sp,
                                    color = Color(0xFF2E7D32),
                                    lineHeight = 26.sp
                                )
                            } else {
                                kbEntry.treatment.chemical.forEach { chem ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "${chem.productExample} (${chem.activeIngredient})",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0D47A1)
                                            )
                                            val doseDisplay = if (chem.doseVerified) chem.dose else stringResource(R.string.follow_product_label)
                                            Text(
                                                text = "💧 Dose: $doseDisplay",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1565C0),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                            val phiInfo = if (chem.phiDays != null) " • Waiting period (PHI): ${chem.phiDays} days" else ""
                                            Text(
                                                text = "🛡️ " + chem.safetyLine.forLanguage(language) + phiInfo,
                                                fontSize = 14.sp,
                                                color = Color(0xFFE65100),
                                                modifier = Modifier.padding(top = 4.dp),
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF3E0),
                                    border = BorderStroke(1.dp, Color(0xFFFFCC80))
                                ) {
                                    Text(
                                        text = "⚠️ " + stringResource(R.string.chemical_safety_warning),
                                        fontSize = 13.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // On-Device Advisory Card (Llama 3.2 AI Explanation)
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
                        advisorSource = advisorSource,
                        audioAdvisor = audioAdvisor,
                        onCancel = onCancelAdvice
                    )

                    // Task CHAT: Offline chat box on the result screen about the detected problem
                    val app = context.applicationContext as? CropDoctorApplication
                    val effectiveAdvisor = advisor ?: app?.getAdvisorIfInitialized()
                    val effectivePassageStore = passageStore ?: app?.getPassageStoreIfInitialized()
                    if (effectiveAdvisor != null && effectivePassageStore != null) {
                        ResultChatSection(
                            classId = result.classId,
                            crop = kbEntry.crop,
                            problemName = kbEntry.names.forLanguage(language),
                            severity = result.severityBand.name,
                            certainty = result.certaintyBand.name,
                            language = language,
                            passageStore = effectivePassageStore,
                            advisor = effectiveAdvisor,
                            isUncertainOrOutOfClass = isUncertain || result.outcome == DiagnosisOutcome.OUT_OF_CLASS,
                            onCallOfficer = { showHelplineDialog = true }
                        )
                    }

                    // Requirement 3: Clear "Call extension officer" button for severe cases
                    if (shouldRefer) {
                        Button(
                            onClick = { showHelplineDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.btn_call_officer),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Share Field Report Button
                    OutlinedButton(
                        onClick = {
                            val reportBuilder = StringBuilder()
                            reportBuilder.append("🌾 Crop Doctor - Field Diagnosis Report\n")
                            reportBuilder.append("====================================\n")
                            reportBuilder.append("Crop: ${kbEntry.crop.replaceFirstChar { it.uppercase() }}\n")
                            reportBuilder.append("Diagnosis: ${kbEntry.names.forLanguage(language)}\n")
                            reportBuilder.append("Severity: ${result.severityBand.name}\n")
                            reportBuilder.append("Certainty: ${result.certaintyBand.name} (${(result.confidence * 100).toInt()}%)\n")
                            reportBuilder.append("Advisor: $advisorSource\n\n")

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, FarmGreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenDark)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = FarmGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_share_report),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Save Case Report Button
                    Button(
                        onClick = onSaveCaseReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Assignment, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_save_case_report),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Take another photo
                    OutlinedButton(
                        onClick = onTakeAnotherPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFC8E6C9)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenDark)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = FarmGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_take_another_photo),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Reusable Section Card with 16dp rounded corners, soft shadow, icon, 18sp bold title, and expandable content.
 */
@Composable
fun ResultSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = FarmGreenPrimary,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C271E)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color(0xFF556056),
                    modifier = Modifier.size(26.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
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
        border = BorderStroke(1.dp, borderColor)
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
 * Helper tuple for 4 values
 */
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
    val immediateStep = kbEntry.immediateStep.forLanguage(language)
    if (immediateStep.isNotBlank()) {
        items.add(AudioPlayItem(text = immediateStep, lang = language))
    }

    val sevAdvice = kbEntry.severityAdvice[result.severityBand.name]?.forLanguage(language) ?: ""
    if (sevAdvice.isNotBlank()) {
        items.add(AudioPlayItem(text = sevAdvice, lang = language))
    }

    if (kbEntry.treatment.natural.isNotEmpty()) {
        val naturalAction = kbEntry.treatment.natural.first().action.forLanguage(language)
        if (naturalAction.isNotBlank()) {
            items.add(AudioPlayItem(text = naturalAction, lang = language))
        }
    }

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
