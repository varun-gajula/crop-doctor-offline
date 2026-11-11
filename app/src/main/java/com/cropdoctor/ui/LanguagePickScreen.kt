package com.cropdoctor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun LanguagePickScreen(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    var selectedLang by remember { mutableStateOf(currentLanguage) }

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
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "Choose Language / భాషను ఎంచుకోండి",
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select your preferred language",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "మీకు సౌకర్యవంతమైన భాషను ఎంచుకోండి",
                    fontSize = 15.sp,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // English Card
                LanguageRadioCard(
                    flag = "🇬🇧",
                    title = "English",
                    subtitle = "(English)",
                    isSelected = selectedLang == "en",
                    onClick = { selectedLang = "en" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Telugu Card
                LanguageRadioCard(
                    flag = "🇮🇳",
                    title = "తెలుగు",
                    subtitle = "(Telugu)",
                    isSelected = selectedLang == "te",
                    onClick = { selectedLang = "te" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hindi Card
                LanguageRadioCard(
                    flag = "🇮🇳",
                    title = "हिन्दी",
                    subtitle = "(Hindi)",
                    isSelected = selectedLang == "hi",
                    onClick = { selectedLang = "hi" }
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Farm decoration at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚜 🌾 🌾 🌾 🌾",
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Continue Button
                Button(
                    onClick = { onLanguageSelected(selectedLang) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Text(
                        text = "Continue  →",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun LanguageRadioCard(
    flag: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) FarmGreenPrimary else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
            )
        }
    }
}
