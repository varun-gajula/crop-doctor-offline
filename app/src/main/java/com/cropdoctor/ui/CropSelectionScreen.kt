package com.cropdoctor.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.ui.components.LanguageChip
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

data class CropTileData(
    val key: String,
    val nameRes: Int,
    val vectorRes: Int,
    val contentDescPrefix: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropSelectionScreen(
    language: String,
    onCropSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    onLanguageChange: (String) -> Unit,
    customTitle: String? = null,
    customSubtitle: String? = null
) {
    BackHandler { onBackClick() }

    val crops = listOf(
        CropTileData("rice", R.string.crop_rice, R.drawable.ic_tile_rice, "Rice"),
        CropTileData("cotton", R.string.crop_cotton, R.drawable.ic_tile_cotton, "Cotton"),
        CropTileData("chilli", R.string.crop_chilli, R.drawable.ic_tile_chilli, "Chilli"),
        CropTileData("tomato", R.string.crop_tomato, R.drawable.ic_tile_tomato, "Tomato"),
        CropTileData("maize", R.string.crop_maize, R.drawable.ic_tile_maize, "Maize")
    )

    val displayTitle = customTitle ?: stringResource(R.string.select_crop_title)
    val displaySubtitle = customSubtitle ?: stringResource(R.string.select_crop_sub)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FarmGreenPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = displayTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Persistent language chip
                LanguageChip(
                    currentLanguage = language,
                    onLanguageSelected = onLanguageChange
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAF7))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Informational header card (matching old Theme and cards)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displaySubtitle,
                            fontSize = 15.sp,
                            color = FarmGreenDark.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }
                    Text(
                        text = "🌱",
                        fontSize = 36.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5 Crop Tiles Layout (2 rows of 2, 1 centered row of 1)
            // Row 1: Rice & Cotton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CropTileItem(
                    cropData = crops[0],
                    language = language,
                    modifier = Modifier.weight(1f),
                    onSelect = { onCropSelected("rice") }
                )
                CropTileItem(
                    cropData = crops[1],
                    language = language,
                    modifier = Modifier.weight(1f),
                    onSelect = { onCropSelected("cotton") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Chilli & Tomato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CropTileItem(
                    cropData = crops[2],
                    language = language,
                    modifier = Modifier.weight(1f),
                    onSelect = { onCropSelected("chilli") }
                )
                CropTileItem(
                    cropData = crops[3],
                    language = language,
                    modifier = Modifier.weight(1f),
                    onSelect = { onCropSelected("tomato") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 3: Maize (centered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CropTileItem(
                    cropData = crops[4],
                    language = language,
                    modifier = Modifier.fillMaxWidth(0.55f),
                    onSelect = { onCropSelected("maize") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CropTileItem(
    cropData: CropTileData,
    language: String,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val cropName = stringResource(cropData.nameRes)
    val tileContentDesc = "Select $cropName tile (${cropData.contentDescPrefix})"

    // Font size: >= 18sp (Telugu 20sp per Rule U2)
    val nameFontSize = if (language == "te") 21.sp else 19.sp

    // Check if custom user JPEG photo exists in assets/crop_tiles/${key}.jpg or .png (Task P3 Requirement 2)
    val customBitmap = remember(cropData.key) {
        try {
            val candidates = listOf(
                "crop_tiles/${cropData.key}.jpg",
                "crop_tiles/${cropData.key}.jpeg",
                "crop_tiles/${cropData.key}.png"
            )
            var bmp: android.graphics.Bitmap? = null
            for (path in candidates) {
                try {
                    context.assets.open(path).use { stream ->
                        bmp = BitmapFactory.decodeStream(stream)
                    }
                    if (bmp != null) break
                } catch (_: Exception) {}
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .heightIn(min = 150.dp) // Strictly >= 96dp (Task P3)
            .clickable(onClickLabel = tileContentDesc) { onSelect() }
            .semantics { contentDescription = tileContentDesc },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFC8E6C9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Crop Picture container
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF1F8F1)),
                contentAlignment = Alignment.Center
            ) {
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap.asImageBitmap(),
                        contentDescription = "$cropName photo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = cropData.vectorRes),
                        contentDescription = "$cropName vector illustration",
                        modifier = Modifier.size(68.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Crop Name: >= 18sp (Telugu 20sp), high contrast
            Text(
                text = cropName,
                fontSize = nameFontSize,
                fontWeight = FontWeight.Bold,
                color = FarmGreenDark,
                textAlign = TextAlign.Center
            )
        }
    }
}
