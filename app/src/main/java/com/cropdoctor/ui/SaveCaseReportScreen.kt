package com.cropdoctor.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.domain.CaseReport
import com.cropdoctor.domain.CaseReportRepository
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaveCaseReportScreen(
    result: ClassifierResult,
    kbEntry: KbEntry,
    language: String,
    region: String,
    capturedBitmap: Bitmap?,
    onBackClick: () -> Unit,
    onViewSavedCases: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isUncertain = result.classId == "unknown" || result.certaintyBand == CertaintyBand.NOT_SURE
    val displayCrop = if (isUncertain) "—" else kbEntry.crop.replaceFirstChar { it.uppercase() }
    val displayProblem = if (isUncertain) {
        stringResource(R.string.cant_tell_no_disease)
    } else {
        kbEntry.names.forLanguage(language)
    }

    val certaintyWord = when (result.certaintyBand) {
        CertaintyBand.SURE -> stringResource(R.string.certainty_sure)
        CertaintyBand.PROBABLY -> stringResource(R.string.certainty_probably)
        CertaintyBand.NOT_SURE -> stringResource(R.string.certainty_not_sure)
    }

    val severityWord = when (result.severityBand) {
        SeverityBand.MILD -> stringResource(R.string.severity_mild)
        SeverityBand.MODERATE -> stringResource(R.string.severity_moderate)
        SeverityBand.SEVERE -> stringResource(R.string.severity_severe)
    }

    val currentDateStr = remember {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date())
    }

    var farmerNote by remember { mutableStateOf("") }
    var savedReport by remember { mutableStateOf<CaseReport?>(null) }
    var isSaved by remember { mutableStateOf(false) }

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
                        text = stringResource(R.string.save_case_report_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Thumbnail Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                        Text(text = "🌱", fontSize = 64.sp)
                    }
                }
            }

            // Structured Case Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Crop
                    ReportDetailRow(
                        label = stringResource(R.string.report_crop),
                        value = displayCrop
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Finding / Label
                    ReportDetailRow(
                        label = stringResource(R.string.report_problem),
                        value = displayProblem,
                        isHighlight = true
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Certainty (in words: Sure / Probably / Not sure)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.report_certainty),
                            fontSize = 15.sp,
                            color = Color(0xFF555555),
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (cColor, cIcon) = when (result.certaintyBand) {
                                CertaintyBand.SURE -> Pair(Color(0xFF2E7D32), Icons.Default.CheckCircle)
                                CertaintyBand.PROBABLY -> Pair(Color(0xFFD97706), Icons.Default.Info)
                                CertaintyBand.NOT_SURE -> Pair(Color(0xFFD32F2F), Icons.Default.Warning)
                            }
                            Icon(
                                imageVector = cIcon,
                                contentDescription = null,
                                tint = cColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = certaintyWord,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = cColor
                            )
                        }
                    }

                    // Severity (Only shown when not uncertain)
                    if (!isUncertain) {
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.report_severity),
                                fontSize = 15.sp,
                                color = Color(0xFF555555),
                                fontWeight = FontWeight.Medium
                            )
                            val sColor = when (result.severityBand) {
                                SeverityBand.MILD -> Color(0xFF2E7D32)
                                SeverityBand.MODERATE -> Color(0xFFD97706)
                                SeverityBand.SEVERE -> Color(0xFFD32F2F)
                            }
                            Text(
                                text = severityWord,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = sColor
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Date & Region
                    ReportDetailRow(
                        label = stringResource(R.string.report_date),
                        value = currentDateStr
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    ReportDetailRow(
                        label = stringResource(R.string.report_region),
                        value = region.ifEmpty { "India" }
                    )
                }
            }

            // Farmer Note Input Field
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_note),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = farmerNote,
                        onValueChange = { farmerNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.report_note_hint),
                                fontSize = 14.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Save Locally Action Button
            Button(
                onClick = {
                    val report = CaseReportRepository.saveCaseReport(
                        context = context,
                        crop = displayCrop,
                        problemLabel = displayProblem,
                        certaintyWord = certaintyWord,
                        severity = if (isUncertain) "N/A" else severityWord,
                        region = region.ifEmpty { "India" },
                        note = farmerNote,
                        bitmap = capturedBitmap,
                        isUncertain = isUncertain,
                        classId = result.classId
                    )
                    savedReport = report
                    isSaved = true
                    Toast.makeText(
                        context,
                        context.getString(R.string.case_report_saved_success),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) Color(0xFF2E7D32) else FarmGreenPrimary
                )
            ) {
                Text(
                    text = if (isSaved) "✓ " + stringResource(R.string.btn_saved_locally) else "💾 " + stringResource(R.string.btn_save_locally),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Share Report Button (Android Share Sheet)
            OutlinedButton(
                onClick = {
                    val reportToShare = savedReport ?: CaseReport(
                        crop = displayCrop,
                        problemLabel = displayProblem,
                        certaintyWord = certaintyWord,
                        severity = if (isUncertain) "N/A" else severityWord,
                        region = region.ifEmpty { "India" },
                        dateString = currentDateStr,
                        note = farmerNote,
                        isUncertain = isUncertain,
                        classId = result.classId
                    )
                    CaseReportRepository.shareCaseReport(context, reportToShare)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = FarmGreenPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_share_report),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // View All Saved Cases Link Button
            TextButton(
                onClick = onViewSavedCases,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "📁 " + stringResource(R.string.saved_cases_title) + " →",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReportDetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color(0xFF555555),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 17.sp else 15.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) FarmGreenDark else Color(0xFF212121),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

