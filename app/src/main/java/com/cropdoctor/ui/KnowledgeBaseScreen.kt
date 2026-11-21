package com.cropdoctor.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBase
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
            // Not found, check next extension
        }
    }
    return null
}

@Composable
fun KnowledgeBaseScreen(
    crop: String,
    language: String,
    onBackClick: () -> Unit,
    onProblemClick: (KbEntry) -> Unit,
    onChangeCropClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current

    val allEntries = remember { KnowledgeBase.getAllEntries() }
    // Only non-healthy problem entries for this crop
    val cropProblems = remember(crop, allEntries) {
        allEntries.filter {
            it.crop.equals(crop, ignoreCase = true) && it.type != "healthy"
        }
    }

    val cropNameRes = when (crop.lowercase()) {
        "rice" -> R.string.crop_rice
        "cotton" -> R.string.crop_cotton
        "chilli" -> R.string.crop_chilli
        "tomato" -> R.string.crop_tomato
        "maize" -> R.string.crop_maize
        else -> R.string.crop_auto
    }
    val cropDisplayName = stringResource(cropNameRes)

    // Standard Growth Stages
    val stageOrder = listOf(
        "Seedling",
        "Vegetative",
        "Flowering",
        "Fruiting or Grain filling",
        "Any stage"
    )

    var selectedStageFilter by remember { mutableStateOf("ALL") }

    // Grouping by stage: entries with null/empty stage map to "Any stage"
    val groupedProblems = remember(cropProblems) {
        val groups = LinkedHashMap<String, MutableList<KbEntry>>()
        for (st in stageOrder) {
            groups[st] = mutableListOf()
        }
        for (entry in cropProblems) {
            val stKey = when (entry.stage) {
                "Seedling" -> "Seedling"
                "Vegetative" -> "Vegetative"
                "Flowering" -> "Flowering"
                "Fruiting or Grain filling", "Grain filling", "Fruiting" -> "Fruiting or Grain filling"
                else -> "Any stage"
            }
            groups.getOrPut(stKey) { mutableListOf() }.add(entry)
        }
        groups.filter { it.value.isNotEmpty() }
    }

    // Expansion state for each stage (default is expanded)
    val expandedStages = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(groupedProblems) {
        groupedProblems.keys.forEach { stage ->
            if (!expandedStages.containsKey(stage)) {
                expandedStages[stage] = true
            }
        }
    }

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
                        text = stringResource(R.string.problems_of_crop, cropDisplayName),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Change crop action chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { onChangeCropClick() }
                ) {
                    Text(
                        text = stringResource(R.string.btn_change_crop),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
            // Horizontal Growth Stage Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStageFilter == "ALL",
                    onClick = { selectedStageFilter = "ALL" },
                    label = {
                        Text(
                            text = stringResource(R.string.stage_all) + " (${cropProblems.size})",
                            fontSize = 14.sp,
                            fontWeight = if (selectedStageFilter == "ALL") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmGreenPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                stageOrder.forEach { stageKey ->
                    val count = groupedProblems[stageKey]?.size ?: 0
                    if (count > 0) {
                        val isSelected = selectedStageFilter == stageKey
                        val localizedStageName = when (stageKey) {
                            "Seedling" -> stringResource(R.string.stage_seedling)
                            "Vegetative" -> stringResource(R.string.stage_vegetative)
                            "Flowering" -> stringResource(R.string.stage_flowering)
                            "Fruiting or Grain filling" -> stringResource(R.string.stage_fruiting_grain_filling)
                            else -> stringResource(R.string.stage_any)
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStageFilter = stageKey },
                            label = {
                                Text(
                                    text = "$localizedStageName ($count)",
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmGreenPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color(0xFF333333)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Content list grouped by growth stage
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                groupedProblems.forEach { (stageKey, entriesInStage) ->
                    if (selectedStageFilter == "ALL" || selectedStageFilter == stageKey) {
                        item(key = "header_$stageKey") {
                            val isExpanded = expandedStages[stageKey] ?: true
                            val stageTitle = when (stageKey) {
                                "Seedling" -> "🌱 " + stringResource(R.string.stage_seedling)
                                "Vegetative" -> "🌿 " + stringResource(R.string.stage_vegetative)
                                "Flowering" -> "🌸 " + stringResource(R.string.stage_flowering)
                                "Fruiting or Grain filling" -> "🌾 " + stringResource(R.string.stage_fruiting_grain_filling)
                                else -> "🔄 " + stringResource(R.string.stage_any)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stageTitle,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FarmGreenDark
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            text = "${entriesInStage.size}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FarmGreenPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // View all / Collapse toggle button
                                TextButton(
                                    onClick = {
                                        expandedStages[stageKey] = !isExpanded
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isExpanded) stringResource(R.string.btn_collapse) else stringResource(R.string.btn_view_all),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FarmGreenPrimary
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = FarmGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Items for this stage with expand/collapse animation
                        val isExpanded = expandedStages[stageKey] ?: true
                        if (isExpanded) {
                            items(entriesInStage, key = { it.classId }) { entry ->
                                ProblemCard(
                                    entry = entry,
                                    language = language,
                                    context = context,
                                    onClick = { onProblemClick(entry) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemCard(
    entry: KbEntry,
    language: String,
    context: Context,
    onClick: () -> Unit
) {
    val guideBitmap = remember(entry.classId) { loadGuidePhoto(context, entry.classId) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0E0E0)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo container or vector icon fallback
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                if (guideBitmap != null) {
                    Image(
                        bitmap = guideBitmap.asImageBitmap(),
                        contentDescription = entry.names.forLanguage(language),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Simple vector leaf icon (per Task F1 Item 4)
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = FarmGreenPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Badges row: Disease/Pest chip + Growth stage chip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Disease or Pest chip
                    if (entry.type == "pest") {
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

                    // Stage chip
                    val stageText = when (entry.stage) {
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

                Spacer(modifier = Modifier.height(4.dp))

                // Problem Name in localized language
                Text(
                    text = entry.names.forLanguage(language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark,
                    lineHeight = 23.sp
                )

                // Pathogen / scientific subtitle
                if (entry.pathogen.isNotBlank() && entry.pathogen != "None") {
                    Text(
                        text = entry.pathogen,
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View Details",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
