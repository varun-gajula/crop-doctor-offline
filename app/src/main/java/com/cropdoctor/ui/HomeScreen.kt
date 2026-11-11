package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun HomeScreen(
    language: String,
    onScanClick: () -> Unit,
    onBrowseDiseasesClick: () -> Unit,
    onFarmingTipsClick: () -> Unit,
    onChangeLanguageClick: () -> Unit,
    onMoreOptionsClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val headerTitle = when (language) {
        "te" -> "అగ్రి కేర్ AI"
        "hi" -> "एग्रीकेयर AI"
        else -> "AgriCare AI"
    }

    val bannerHeading = when (language) {
        "te" -> "మీ చేతుల్లో ఆధునిక పంట రక్షణ"
        "hi" -> "आपके हाथ में स्मार्ट फसल सुरक्षा"
        else -> "Smart Crop Care in Your Hand"
    }

    val bannerSub = when (language) {
        "te" -> "తెగులును గుర్తించి తక్షణ నివారణ పొందండి"
        "hi" -> "पत्ती स्कैन करें और तुरंत समाधान पाएं"
        else -> "Scan a leaf to detect diseases and get solutions."
    }

    val scanTitle = when (language) {
        "te" -> "ఆకును స్కాన్ చేయండి"
        "hi" -> "पत्ती स्कैन करें"
        else -> "Scan Leaf"
    }

    val scanSub = when (language) {
        "te" -> "కెమెరా ద్వారా ఫోటో తీయండి లేదా అప్‌లోడ్ చేయండి"
        "hi" -> "फोटो खींचें या गैलरी से चुनें"
        else -> "Tap to capture or upload an image"
    }

    val browseTitle = when (language) {
        "te" -> "తెగుళ్ల సమాచారం"
        "hi" -> "रोग जानकारी"
        else -> "Browse Diseases"
    }

    val tipsTitle = when (language) {
        "te" -> "వ్యవసాయ చిట్కాలు"
        "hi" -> "कृषि सुझाव"
        else -> "Farming Tips"
    }

    val settingsTitle = when (language) {
        "te" -> "సహాయం & మద్దతు"
        "hi" -> "सहायता"
        else -> "Help & Support"
    }

    val langButtonTitle = when (language) {
        "te" -> "భాష (తెలుగు)"
        "hi" -> "भाषा (हिन्दी)"
        else -> "Language (English)"
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FarmGreenPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMoreOptionsClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                Text(
                    text = headerTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Language switcher in top right corner
                IconButton(onClick = onChangeLanguageClick) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Change Language",
                        tint = Color.White
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already Home */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FarmGreenPrimary,
                        selectedTextColor = FarmGreenPrimary,
                        indicatorColor = Color(0xFFE8F5E9)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onBrowseDiseasesClick,
                    icon = { Icon(Icons.Default.Search, contentDescription = "Knowledge Base") },
                    label = { Text("Diseases", fontSize = 12.sp) }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onFarmingTipsClick,
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Tips") },
                    label = { Text("Tips", fontSize = 12.sp) }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onMoreOptionsClick,
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text("More", fontSize = 12.sp) }
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
            // Hero Banner with Illustration Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                            )
                        )
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bannerHeading,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark,
                            lineHeight = 25.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bannerSub,
                            fontSize = 14.sp,
                            color = Color(0xFF37474F)
                        )
                    }
                    Text(
                        text = "👨‍🌾🌾",
                        fontSize = 44.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Scan Leaf Action Card (Exact match to sample photo)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onScanClick() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF81C784)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Scan",
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = scanTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = scanSub,
                        fontSize = 14.sp,
                        color = Color(0xFF616161)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2x2 Quick Action Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuickActionTile(
                    title = browseTitle,
                    icon = Icons.Default.Search,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = onBrowseDiseasesClick
                )

                QuickActionTile(
                    title = tipsTitle,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f),
                    onClick = onFarmingTipsClick
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuickActionTile(
                    title = settingsTitle,
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    color = Color(0xFF00796B),
                    modifier = Modifier.weight(1f),
                    onClick = onMoreOptionsClick
                )

                QuickActionTile(
                    title = langButtonTitle,
                    icon = Icons.Default.Language,
                    color = Color(0xFFE65100),
                    modifier = Modifier.weight(1f),
                    onClick = onChangeLanguageClick
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF263238)
            )
        }
    }
}

