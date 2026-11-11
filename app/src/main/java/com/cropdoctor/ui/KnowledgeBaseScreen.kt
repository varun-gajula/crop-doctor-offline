package com.cropdoctor.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun KnowledgeBaseScreen(
    language: String,
    onBackClick: () -> Unit,
    onDiseaseClick: (KbEntry) -> Unit
) {
    var selectedCrop by remember { mutableStateOf("rice") }
    val crops = listOf(
        Pair("rice", "🌾 Rice"),
        Pair("cotton", "☁️ Cotton"),
        Pair("chilli", "🌶️ Chilli"),
        Pair("tomato", "🍅 Tomato"),
        Pair("maize", "🌽 Maize")
    )

    val allEntries = remember { KnowledgeBase.getAllEntries() }
    val filteredEntries = allEntries.filter { it.crop.equals(selectedCrop, ignoreCase = true) }

    val screenTitle = when (language) {
        "te" -> "పంట తెగుళ్ల విజ్ఞానం"
        "hi" -> "फसल रोग ज्ञानकोष"
        else -> "Knowledge Base"
    }

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
        ) {
            // Horizontal Crop Selector Chips (Exact match to sample photo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                crops.forEach { (cropKey, cropLabel) ->
                    val isSelected = selectedCrop == cropKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCrop = cropKey },
                        label = {
                            Text(
                                text = cropLabel,
                                fontSize = 15.sp,
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

            // Disease List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEntries) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDiseaseClick(entry) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (entry.type == "healthy") "🌱" else if (entry.type == "pest") "🐛" else "🍂",
                                    fontSize = 26.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.names.forLanguage(language),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark,
                                    lineHeight = 22.sp
                                )
                                Text(
                                    text = entry.pathogen,
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "View",
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Informational Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📖", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (language) {
                            "te" -> "పంట తెగుళ్ళు మరియు సంరక్షణ పద్ధతుల గురించి తెలుసుకోండి."
                            "hi" -> "फसल रोगों और देखभाल के तरीकों के बारे में विस्तार से जानें।"
                            else -> "Learn more about crop diseases and care tips"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = FarmGreenDark
                    )
                }
            }
        }
    }
}
