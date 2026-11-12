package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.domain.ScanHistoryRecord
import com.cropdoctor.domain.ScanHistoryRepository
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun ScanHistoryScreen(
    language: String,
    onBackClick: () -> Unit,
    onSelectRecord: (ScanHistoryRecord) -> Unit = {}
) {
    val context = LocalContext.current
    var historyList by remember { mutableStateOf(ScanHistoryRepository.getHistory(context)) }

    val screenTitle = when (language) {
        "te" -> "స్కాన్ చరిత్ర & ట్రాకర్"
        "hi" -> "स्कैन इतिहास और ट्रैकर"
        else -> "Scan History & Tracker"
    }

    val emptyMessage = when (language) {
        "te" -> "గతంలో స్కాన్ చేసిన రికార్డులు లేవు.\nపంట ఆకును స్కాన్ చేసి ఇక్కడ ఫలితాలను ట్రాక్ చేయండి."
        "hi" -> "कोई पिछला स्कैन रिकॉर्ड नहीं मिला।\nपत्ती स्कैन करें और यहाँ इतिहास ट्रैक करें।"
        else -> "No past leaf scans found.\nScan leaves to track disease trends over time."
    }

    val clearHistoryText = when (language) {
        "te" -> "చరిత్రను తొలగించండి"
        "hi" -> "इतिहास हटाएं"
        else -> "Clear History"
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
                        color = Color.White
                    )
                }

                if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            ScanHistoryRepository.clearHistory(context)
                            historyList = emptyList()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = clearHistoryText,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7FAF7))
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFA5D6A7)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = emptyMessage,
                        fontSize = 16.sp,
                        color = Color(0xFF555555),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7FAF7))
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList) { item ->
                    val isHealthy = item.classId.endsWith("_healthy")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRecord(item) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isHealthy) "✓" else "⚠️",
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.diseaseName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHealthy) FarmGreenDark else Color(0xFFB71C1C)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.crop} • ${(item.confidence * 100).toInt()}% match",
                                        fontSize = 13.sp,
                                        color = Color(0xFF616161)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (item.severity) {
                                                    "SEVERE" -> Color(0xFFFFCDD2)
                                                    "MODERATE" -> Color(0xFFFFE0B2)
                                                    else -> Color(0xFFC8E6C9)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.severity,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (item.severity) {
                                                "SEVERE" -> Color(0xFFC62828)
                                                "MODERATE" -> Color(0xFFE65100)
                                                else -> Color(0xFF2E7D32)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📅 ${item.dateString}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

