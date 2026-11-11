package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun AdvisoryCard(
    advisoryText: String,
    isGenerating: Boolean,
    language: String
) {
    val title = when (language) {
        "te" -> "🤖 AI సలహాదారు (On-Device Advisor)"
        "hi" -> "🤖 ऑन-डिवाइस कृषि सलाहकार"
        else -> "🤖 On-Device Llama Advisory"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFC8E6C9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenPrimary
                )
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = FarmGreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (advisoryText.isEmpty() && isGenerating) {
                    when (language) {
                        "te" -> "AI సలహా సిద్ధమవుతోంది..."
                        "hi" -> "सलाह तैयार हो रही है..."
                        else -> "Generating advice on-device..."
                    }
                } else advisoryText + (if (isGenerating) " ▌" else ""),
                fontSize = 17.sp,
                color = Color(0xFF263238),
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Built with Llama 3.2 1B • ExecuTorch (CPU)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF689F38)
                )
            }
        }
    }
}
