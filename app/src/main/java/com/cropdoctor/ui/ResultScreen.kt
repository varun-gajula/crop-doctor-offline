package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.domain.TreatmentSelector
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.theme.*

@Composable
fun ResultScreen(
    result: ClassifierResult,
    kbEntry: KbEntry,
    language: String,
    advisoryText: String,
    isGeneratingAdvice: Boolean,
    onRetakeClick: () -> Unit,
    onChangeLanguageClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val initialTab = TreatmentSelector.getInitialTab(result.severityBand, result.classId)
    val shouldRefer = RulesEngine.shouldReferToKVK(result.severityBand, kbEntry.referralThreshold)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FBF7))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌱 Crop Doctor",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = FarmGreenPrimary
            )
            TextButton(onClick = onChangeLanguageClick) {
                Text(
                    text = "🌐 " + language.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Diagnosis Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = kbEntry.names.forLanguage(language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color(0xFF1B5E20),
                    lineHeight = 32.sp
                )

                if (kbEntry.pathogen != "None") {
                    Text(
                        text = "Pathogen: ${kbEntry.pathogen}",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Badges row: Severity + Certainty
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Severity Badge
                    val (sevBg, sevText, sevLabel) = when (result.severityBand) {
                        SeverityBand.MILD -> Triple(
                            SeverityMildBg,
                            SeverityMildText,
                            when (language) { "te" -> "✓ తేలికపాటి (Mild)"; "hi" -> "✓ हल्का (Mild)"; else -> "✓ Mild" }
                        )
                        SeverityBand.MODERATE -> Triple(
                            SeverityModerateBg,
                            SeverityModerateText,
                            when (language) { "te" -> "⚠ మధ్యస్థం (Moderate)"; "hi" -> "⚠ मध्यम (Moderate)"; else -> "⚠ Moderate" }
                        )
                        SeverityBand.SEVERE -> Triple(
                            SeveritySevereBg,
                            SeveritySevereText,
                            when (language) { "te" -> "🚨 తీవ్రమైనది (Severe)"; "hi" -> "🚨 गंभीर (Severe)"; else -> "🚨 Severe" }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(sevBg, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sevLabel,
                            color = sevText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Certainty Badge
                    val certLabel = when (result.certaintyBand) {
                        CertaintyBand.SURE -> when (language) { "te" -> "ఖచ్చితంగా (Sure)"; "hi" -> "निश्चित (Sure)"; else -> "Sure" }
                        CertaintyBand.PROBABLY -> when (language) { "te" -> "బహుశా (Probably)"; "hi" -> "संभवतः (Probably)"; else -> "Probably" }
                        CertaintyBand.NOT_SURE -> when (language) { "te" -> "సందేహాస్పదం"; "hi" -> "संदेहजनक"; else -> "Not sure" }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8EAF6), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = certLabel,
                            color = Color(0xFF283593),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Immediate Step Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFDE7), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = when (language) {
                                "te" -> "⚡ తక్షణ చర్య:"
                                "hi" -> "⚡ तुरंत करने योग्य कदम:"
                                else -> "⚡ Immediate First Step:"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFF57F17)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = kbEntry.immediateStep.forLanguage(language),
                            fontSize = 17.sp,
                            color = Color(0xFF212121),
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Two-Tier Treatment Tabs
        TreatmentSection(
            kbEntry = kbEntry,
            language = language,
            initialTab = initialTab
        )

        Spacer(modifier = Modifier.height(14.dp))

        // On-Device Llama Advisory
        AdvisoryCard(
            advisoryText = advisoryText,
            isGenerating = isGeneratingAdvice,
            language = language
        )

        // Extension Worker Referral Button if Severe
        if (shouldRefer) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { /* Extension worker call action */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text(
                    text = when (language) {
                        "te" -> "📞 వ్యవసాయాధికారిని సంప్రదించండి (KVK Helpline)"
                        "hi" -> "📞 कृषि विज्ञान केंद्र / विशेषज्ञ से संपर्क करें"
                        else -> "📞 Contact KVK Extension Specialist"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Retake / Scan Next Button
        Button(
            onClick = onRetakeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                text = when (language) {
                    "te" -> "📷 మరొక మొక్కను పరీక్షించండి"
                    "hi" -> "📷 दूसरे पौधे की जांच करें"
                    else -> "📷 Scan Another Plant"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
