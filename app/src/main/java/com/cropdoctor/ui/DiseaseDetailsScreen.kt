package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
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
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun DiseaseDetailsScreen(
    kbEntry: KbEntry,
    language: String,
    onBackClick: () -> Unit,
    onBackToHome: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: About, 1: Symptoms, 2: Prevention

    val tabTitles = when (language) {
        "te" -> listOf("వివరాలు", "లక్షణాలు", "నివారణ")
        "hi" -> listOf("विवरण", "लक्षण", "रोकथाम")
        else -> listOf("About", "Symptoms", "Prevention")
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
                    text = when (language) { "te" -> "తెగులు పూర్తి వివరాలు"; "hi" -> "रोग का संपूर्ण विवरण"; else -> "Disease Details" },
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header Card with Thumbnail & Title
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌿", fontSize = 38.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = kbEntry.names.forLanguage(language),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark,
                            lineHeight = 26.sp
                        )
                        Text(
                            text = "(${kbEntry.crop.replaceFirstChar { it.uppercase() }})",
                            fontSize = 15.sp,
                            color = Color(0xFF666666)
                        )
                        if (kbEntry.pathogen != "None") {
                            Text(
                                text = kbEntry.pathogen,
                                fontSize = 13.sp,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Tabs (About, Symptoms, Prevention)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = FarmGreenPrimary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    when (selectedTab) {
                        0 -> {
                            // ABOUT
                            Text(
                                text = kbEntry.explainText.forLanguage(language),
                                fontSize = 16.sp,
                                color = Color(0xFF333333),
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Pathogen: ${kbEntry.pathogen}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF555555)
                            )
                        }
                        1 -> {
                            // SYMPTOMS
                            Text(
                                text = when (language) { "te" -> "ముఖ్య లక్షణాలు:"; "hi" -> "प्रमुख लक्षण:"; else -> "Key Symptoms:" },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            kbEntry.severityAdvice.forEach { (severity, text) ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$severity: ${text.forLanguage(language)}",
                                        fontSize = 15.sp,
                                        color = Color(0xFF333333),
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                        2 -> {
                            // PREVENTION
                            Text(
                                text = when (language) { "te" -> "నివారణ & రక్షణ పద్ధతులు:"; "hi" -> "रोकथाम और सुरक्षा सुझाव:"; else -> "Prevention & Management:" },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            kbEntry.treatment.natural.forEach { step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = FarmGreenPrimary,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Back to Home Button
            Button(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (language) { "te" -> "హోమ్‌కు తిరిగి వెళ్ళండి"; "hi" -> "होम पर वापस जाएं"; else -> "Back to Home" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

