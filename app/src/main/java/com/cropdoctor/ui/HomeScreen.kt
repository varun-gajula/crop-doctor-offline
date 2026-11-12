package com.cropdoctor.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.ui.components.LanguageChip
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun HomeScreen(
    language: String,
    onScanClick: () -> Unit,
    onBrowseDiseasesClick: () -> Unit,
    onFarmingTipsClick: () -> Unit,
    onChangeLanguageClick: (String) -> Unit,
    onMoreOptionsClick: () -> Unit,
    onChatbotClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_agricare_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Persistent language chip on top bar (T2)
                LanguageChip(
                    currentLanguage = language,
                    onLanguageSelected = { newLang ->
                        onChangeLanguageClick(newLang)
                    }
                )
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
                    label = { Text(stringResource(R.string.nav_home), fontSize = 12.sp) },
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
                    label = { Text(stringResource(R.string.nav_diseases), fontSize = 12.sp) }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onFarmingTipsClick,
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Tips") },
                    label = { Text(stringResource(R.string.nav_tips), fontSize = 12.sp) }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onMoreOptionsClick,
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text(stringResource(R.string.nav_more), fontSize = 12.sp) }
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
            // Hero Banner
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
                            text = stringResource(R.string.home_banner_title),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark,
                            lineHeight = 25.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_banner_sub),
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

            // Primary Scan Leaf Action Card
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
                        text = stringResource(R.string.scan_leaf),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.scan_leaf_sub),
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
                    title = stringResource(R.string.browse_diseases),
                    icon = Icons.Default.Search,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = onBrowseDiseasesClick
                )

                QuickActionTile(
                    title = stringResource(R.string.farming_tips),
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
                    title = stringResource(R.string.settings),
                    icon = Icons.Default.Settings,
                    color = Color(0xFF00796B),
                    modifier = Modifier.weight(1f),
                    onClick = onMoreOptionsClick
                )

                val currentLangLabel = when (language) {
                    "te" -> "భాష (తెలుగు)"
                    "hi" -> "भाषा (हिन्दी)"
                    else -> "Language (English)"
                }
                QuickActionTile(
                    title = currentLangLabel,
                    icon = Icons.Default.Language,
                    color = Color(0xFFE65100),
                    modifier = Modifier.weight(1f),
                    onClick = { onChangeLanguageClick(if (language == "en") "te" else if (language == "te") "hi" else "en") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Doctor Chatbot Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChatbotClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "👨‍🌾", fontSize = 34.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.chat_ai_doctor),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                        Text(
                            text = stringResource(R.string.offline_badge),
                            fontSize = 13.sp,
                            color = Color(0xFF555555)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = FarmGreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                lineHeight = 18.sp
            )
        }
    }
}
