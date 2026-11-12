package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun FirstLaunchScreen(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onComplete: (language: String, region: String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Language, 2: Region
    var selectedLang by remember { mutableStateOf(currentLanguage) }
    var selectedRegion by remember { mutableStateOf("Telangana") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF7))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (step == 1) {
            // STEP 1: LANGUAGE SELECTION (Three big buttons: Telugu, Hindi, English)
            Text(
                text = "🌱",
                fontSize = 54.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crop Doctor",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FarmGreenDark
            )

            Text(
                text = "100% Offline Crop Advisory",
                fontSize = 16.sp,
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Choose Language / భాషను ఎంచుకోండి / भाषा चुनें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Big Button 1: Telugu (తెలుగు)
            BigLanguageButton(
                nativeLabel = "తెలుగు",
                subLabel = "Telugu",
                isSelected = selectedLang == "te",
                onClick = {
                    selectedLang = "te"
                    onLanguageChange("te")
                    step = 2
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Big Button 2: Hindi (हिन्दी)
            BigLanguageButton(
                nativeLabel = "हिन्दी",
                subLabel = "Hindi",
                isSelected = selectedLang == "hi",
                onClick = {
                    selectedLang = "hi"
                    onLanguageChange("hi")
                    step = 2
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Big Button 3: English
            BigLanguageButton(
                nativeLabel = "English",
                subLabel = "English",
                isSelected = selectedLang == "en",
                onClick = {
                    selectedLang = "en"
                    onLanguageChange("en")
                    step = 2
                }
            )
        } else {
            // STEP 2: REGION SELECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { step = 1 }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FarmGreenDark
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.choose_region),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.choose_region_sub),
                fontSize = 15.sp,
                color = Color(0xFF555555),
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val regions = listOf(
                "Telangana" to R.string.region_telangana,
                "Andhra Pradesh" to R.string.region_andhra,
                "Karnataka" to R.string.region_karnataka,
                "Tamil Nadu" to R.string.region_tamil_nadu,
                "Other" to R.string.region_other
            )

            regions.forEach { (regKey, strRes) ->
                BigRegionButton(
                    label = stringResource(strRes),
                    isSelected = selectedRegion == regKey,
                    onClick = { selectedRegion = regKey }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onComplete(selectedLang, selectedRegion) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
            ) {
                Text(
                    text = stringResource(R.string.btn_continue),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BigLanguageButton(
    nativeLabel: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) FarmGreenPrimary else Color(0xFFDDDDDD),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = nativeLabel,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark
                )
                Text(
                    text = subLabel,
                    fontSize = 15.sp,
                    color = Color(0xFF666666)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FarmGreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BigRegionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) FarmGreenPrimary else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) FarmGreenPrimary else Color(0xFF888888),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF263238)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FarmGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
