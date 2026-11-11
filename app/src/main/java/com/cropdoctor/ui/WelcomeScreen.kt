package com.cropdoctor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun WelcomeScreen(
    language: String,
    onGetStarted: () -> Unit,
    onAboutClick: () -> Unit
) {
    val titleText = when (language) {
        "te" -> "అగ్రి కేర్ AI"
        "hi" -> "एग्रीकेयर AI"
        else -> "AgriCare AI"
    }

    val tagline = when (language) {
        "te" -> "గుర్తించండి • నేర్చుకోండి • రక్షించండి"
        "hi" -> "पहचानें • सीखें • सुरक्षित करें"
        else -> "Detect • Learn • Protect"
    }

    val subtitle = when (language) {
        "te" -> "ఆరోగ్యకరమైన పంటలు, సంపన్నమైన రైతులు"
        "hi" -> "स्वस्थ फसलें, समृद्ध किसान"
        else -> "Healthy Crops, Prosperous Farmers"
    }

    val getStartedText = when (language) {
        "te" -> "ప్రారంభించండి  →"
        "hi" -> "शुरू करें  →"
        else -> "Get Started  →"
    }

    val aboutText = when (language) {
        "te" -> "యాప్ గురించి"
        "hi" -> "ऐप के बारे में"
        else -> "About App"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFF4F9F4),
                        Color(0xFFE0F2F1)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Leaf with Circuit Emblem
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(FarmGreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "AgriCare AI Logo",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = titleText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FarmGreenDark,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = tagline,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmGreenPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    color = Color(0xFF4A5568),
                    textAlign = TextAlign.Center
                )
            }

            // Decorative farm landscape card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9).copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🌾 🌽 🌶️ 🍅 ☁️",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "100% Offline Crop Doctor\nArm Bharat AI-SoC Challenge",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = FarmGreenDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Text(
                        text = getStartedText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenPrimary),
                    border = BorderStroke(1.5.dp, FarmGreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = aboutText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
