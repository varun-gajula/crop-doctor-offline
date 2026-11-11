package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.domain.TreatmentSelector.TreatmentTab
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun TreatmentSection(
    kbEntry: KbEntry,
    language: String,
    initialTab: TreatmentTab
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    val naturalTabTitle = when (language) {
        "te" -> "🌿 సహజ నివారణ"
        "hi" -> "🌿 जैविक व देशी"
        else -> "🌿 Natural / Bio"
    }

    val chemicalTabTitle = when (language) {
        "te" -> "🧪 రసాయన మందులు"
        "hi" -> "🧪 रासायनिक दवा"
        else -> "🧪 Chemical"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5E9), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { selectedTab = TreatmentTab.NATURAL },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == TreatmentTab.NATURAL) FarmGreenPrimary else Color.Transparent,
                        contentColor = if (selectedTab == TreatmentTab.NATURAL) Color.White else Color(0xFF333333)
                    ),
                    elevation = null
                ) {
                    Text(text = naturalTabTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Button(
                    onClick = { selectedTab = TreatmentTab.CHEMICAL },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == TreatmentTab.CHEMICAL) Color(0xFF1565C0) else Color.Transparent,
                        contentColor = if (selectedTab == TreatmentTab.CHEMICAL) Color.White else Color(0xFF333333)
                    ),
                    elevation = null
                ) {
                    Text(text = chemicalTabTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                TreatmentTab.NATURAL -> {
                    NaturalTreatmentView(kbEntry, language)
                }
                TreatmentTab.CHEMICAL -> {
                    ChemicalTreatmentView(kbEntry, language)
                }
            }
        }
    }
}

@Composable
fun NaturalTreatmentView(kbEntry: KbEntry, language: String) {
    val steps = kbEntry.treatment.natural
    if (steps.isEmpty()) {
        Text(
            text = when (language) {
                "te" -> "పంట ఆరోగ్యంగా ఉంది. ప్రత్యేక చికిత్స అవసరం లేదు."
                "hi" -> "फसल स्वस्थ है। किसी विशेष उपचार की आवश्यकता नहीं है।"
                else -> "Crop is healthy. No treatment required."
            },
            fontSize = 18.sp,
            color = Color(0xFF2E7D32)
        )
        return
    }

    Column {
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FarmGreenPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = step.action.forLanguage(language),
                        fontSize = 18.sp,
                        color = Color(0xFF212121),
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "Source: ${step.source}",
                        fontSize = 13.sp,
                        color = Color(0xFF757575),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (index < steps.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE0E0E0))
            }
        }
    }
}

@Composable
fun ChemicalTreatmentView(kbEntry: KbEntry, language: String) {
    val chemicals = kbEntry.treatment.chemical
    if (chemicals.isEmpty()) {
        Text(
            text = when (language) {
                "te" -> "ఆరోగ్యకరమైన పంటకు రసాయన మందులు అవసరం లేదు."
                "hi" -> "स्वस्थ फसल के लिए किसी रासायनिक दवा की आवश्यकता नहीं है।"
                else -> "Healthy crop requires no chemical applications."
            },
            fontSize = 18.sp,
            color = Color(0xFF2E7D32)
        )
        return
    }

    Column {
        chemicals.forEachIndexed { index, chem ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = chem.activeIngredient,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF0D47A1)
                    )
                    if (chem.productExample.isNotEmpty()) {
                        Text(
                            text = "Market Example: ${chem.productExample}",
                            fontSize = 15.sp,
                            color = Color(0xFF555555),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dose display
                    val doseLabel = when (language) {
                        "te" -> "మోతాదు: "
                        "hi" -> "मात्रा: "
                        else -> "Dosage: "
                    }
                    val doseText = if (chem.doseVerified) chem.dose else when (language) {
                        "te" -> "ఉత్పత్తి లేబుల్ పాటించండి (Follow label)"
                        "hi" -> "उत्पाद लेबल के अनुसार प्रयोग करें"
                        else -> "Follow CIB&RC product label strictly"
                    }

                    Text(
                        text = doseLabel + doseText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Safety alert box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠ " + chem.safetyLine.forLanguage(language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE65100)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Authority: ${chem.source} (${chem.sourceQuote})",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }
    }
}

