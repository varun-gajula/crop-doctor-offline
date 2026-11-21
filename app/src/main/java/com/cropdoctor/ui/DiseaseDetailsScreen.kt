package com.cropdoctor.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenLight
import com.cropdoctor.ui.theme.FarmGreenPrimary

private fun loadGuidePhoto(context: Context, classId: String): Bitmap? {
    val extensions = listOf("jpg", "jpeg", "png", "webp")
    for (ext in extensions) {
        try {
            context.assets.open("guide/$classId.$ext").use { stream ->
                return BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            // Not found
        }
    }
    return null
}

@Composable
fun DiseaseDetailsScreen(
    kbEntry: KbEntry,
    language: String,
    onBackClick: () -> Unit,
    onBackToHome: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val guideBitmap = remember(kbEntry.classId) { loadGuidePhoto(context, kbEntry.classId) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FarmGreenPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.crop_guide_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onBackToHome) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.White
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card with Image (photo or vector leaf) + Problem Name + Badges
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0E0E0)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (guideBitmap != null) {
                                Image(
                                    bitmap = guideBitmap.asImageBitmap(),
                                    contentDescription = kbEntry.names.forLanguage(language),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = null,
                                    tint = FarmGreenPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Disease/Pest badge + Stage badge
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (kbEntry.type == "pest") {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFBE9E7)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BugReport,
                                                contentDescription = null,
                                                tint = Color(0xFFD84315),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = stringResource(R.string.chip_pest),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD84315)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Coronavirus,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = stringResource(R.string.chip_disease),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }

                                val stageText = when (kbEntry.stage) {
                                    "Seedling" -> stringResource(R.string.stage_seedling)
                                    "Vegetative" -> stringResource(R.string.stage_vegetative)
                                    "Flowering" -> stringResource(R.string.stage_flowering)
                                    "Fruiting or Grain filling", "Grain filling", "Fruiting" -> stringResource(R.string.stage_fruiting_grain_filling)
                                    else -> stringResource(R.string.stage_any)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF3E5F5)
                                ) {
                                    Text(
                                        text = stageText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF6A1B9A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = kbEntry.names.forLanguage(language),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark,
                                lineHeight = 28.sp
                            )

                            if (kbEntry.pathogen.isNotBlank() && kbEntry.pathogen != "None") {
                                Text(
                                    text = kbEntry.pathogen,
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Advice Tiers matching ResultScreen (prevention, natural, chemical)

            // Tier 1: What it is (collapsed by default)
            DetailSectionCard(
                title = stringResource(R.string.section_what_it_is),
                icon = Icons.Default.Spa,
                initiallyExpanded = false
            ) {
                Text(
                    text = kbEntry.explainText.forLanguage(language),
                    fontSize = 18.sp,
                    color = Color(0xFF1B2E1E),
                    lineHeight = 26.sp
                )
                if (kbEntry.earlyWarningSigns.forLanguage(language).isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.watch_for_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = kbEntry.earlyWarningSigns.forLanguage(language),
                        fontSize = 16.sp,
                        color = Color(0xFF444444),
                        lineHeight = 24.sp
                    )
                }
            }

            // Tier 2: What to do now (expanded by default)
            DetailSectionCard(
                title = stringResource(R.string.section_what_to_do_now),
                icon = Icons.Default.PlayArrow,
                initiallyExpanded = true
            ) {
                Text(
                    text = kbEntry.immediateStep.forLanguage(language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B2E1E),
                    lineHeight = 26.sp
                )
            }

            // Tier 3: Prevention & Field Practices (collapsed by default)
            DetailSectionCard(
                title = stringResource(R.string.section_prevention),
                icon = Icons.Default.Shield,
                initiallyExpanded = false
            ) {
                val prevText = kbEntry.preventionAndFieldPractice.forLanguage(language)
                if (prevText.isNotBlank()) {
                    Text(
                        text = prevText,
                        fontSize = 18.sp,
                        color = Color(0xFF1B2E1E),
                        lineHeight = 26.sp
                    )
                } else {
                    Text(
                        text = "Practice clean cultivation, balanced fertilization, and good field drainage.",
                        fontSize = 18.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            // Tier 4: Natural & Traditional Treatments (collapsed by default)
            DetailSectionCard(
                title = stringResource(R.string.section_natural),
                icon = Icons.Default.CheckCircle,
                initiallyExpanded = false
            ) {
                if (kbEntry.treatment.natural.isNotEmpty()) {
                    kbEntry.treatment.natural.forEachIndexed { idx, step ->
                        if (idx > 0) Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFC8E6C9)))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${step.step}. ${step.action.forLanguage(language)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1B5E20),
                                    lineHeight = 25.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.source_label, step.source),
                                    fontSize = 13.sp,
                                    color = Color(0xFF4E6551),
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (step.sourceQuote.isNotBlank()) {
                                    Text(
                                        text = "\"${step.sourceQuote}\"",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No specific natural treatments recorded.",
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Tier 5: Chemical (Advanced) (collapsed by default)
            DetailSectionCard(
                title = stringResource(R.string.section_chemical),
                icon = Icons.Default.LocalPharmacy,
                initiallyExpanded = false
            ) {
                if (kbEntry.treatment.chemical.isNotEmpty()) {
                    // Mandatory chemical safety warning
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.chemical_safety_warning),
                                fontSize = 14.sp,
                                color = Color(0xFFE65100),
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    kbEntry.treatment.chemical.forEachIndexed { idx, step ->
                        if (idx > 0) Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0E0E0)))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = step.activeIngredient,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B2E1E)
                                )
                                if (step.productExample.isNotBlank()) {
                                    Text(
                                        text = "Example product: ${step.productExample}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF555555)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dose: ${if (step.doseVerified) step.dose else stringResource(R.string.follow_product_label)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (step.doseVerified) Color(0xFF1B5E20) else Color(0xFFD97706)
                                )
                                if (step.phiDays != null && step.phiDays > 0) {
                                    Text(
                                        text = "Waiting period before harvest (PHI): ${step.phiDays} days",
                                        fontSize = 14.sp,
                                        color = Color(0xFF555555)
                                    )
                                }
                                Text(
                                    text = "Safety: ${step.safetyLine.forLanguage(language)}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF444444),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = stringResource(R.string.source_label, step.source),
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_chemical_advice),
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Extension officer referral button
            Button(
                onClick = {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001801551"))
                        context.startActivity(dialIntent)
                    } catch (e: Exception) {
                        Log.e("DiseaseDetailsScreen", "Failed to launch dialer", e)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_call_officer),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0E0E0)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = FarmGreenPrimary
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    content()
                }
            }
        }
    }
}
