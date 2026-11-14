package com.cropdoctor.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.CropDoctorApplication
import com.cropdoctor.advisor.ExecuTorchLlamaAdvisor
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MoreOptionsScreen(
    language: String,
    onBackClick: () -> Unit,
    onFarmingTipsClick: () -> Unit,
    onBrowseDiseasesClick: () -> Unit,
    onChatbotClick: () -> Unit = {},
    onScanHistoryClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val importModelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    val displayName = cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) it.getString(nameIndex) else null
                        } else null
                    } ?: "model_import.bin"

                    val targetFile = if (displayName.endsWith(".model") || displayName.contains("tokenizer", ignoreCase = true)) {
                        File(context.filesDir, "tokenizer.model")
                    } else if (displayName.endsWith(".pte")) {
                        File(context.filesDir, "llama-3.2-1b-spinquant.pte")
                    } else {
                        File(context.filesDir, displayName)
                    }

                    contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val app = context.applicationContext as? CropDoctorApplication
                    (app?.advisor as? ExecuTorchLlamaAdvisor)?.findModelFiles()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Imported: ${targetFile.name} (${targetFile.length() / (1024 * 1024)} MB)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val screenTitle = when (language) {
        "te" -> "మరిన్ని ఎంపికలు"
        "hi" -> "अन्य विकल्प"
        else -> "More Options"
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
                    text = screenTitle,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Saved Case Reports (T3)
                MoreOptionItem(
                    icon = Icons.Default.History,
                    iconBg = Color(0xFFE8F5E9),
                    iconTint = FarmGreenPrimary,
                    title = when (language) { "te" -> "భద్రపరిచిన కేస్ నివేదికలు"; "hi" -> "सहेजी गई केस रिपोर्ट"; else -> "Saved Case Reports" },
                    subtitle = when (language) { "te" -> "సేవ్ చేసిన వ్యాధి నివేదికలు & షేర్"; "hi" -> "सहेजी गई रिपोर्ट और शेयर"; else -> "View saved reports & share" },
                    onClick = onScanHistoryClick
                )

                // Favorite Diseases
                MoreOptionItem(
                    icon = Icons.Default.Favorite,
                    iconBg = Color(0xFFFFEBEE),
                    iconTint = Color(0xFFE91E63),
                    title = when (language) { "te" -> "ముఖ్యమైన తెగుళ్లు"; "hi" -> "पसंदीदा रोग"; else -> "Favorite Diseases" },
                    subtitle = when (language) { "te" -> "త్వరిత పరిశీలన"; "hi" -> "त्वरित पहुंच"; else -> "Quick Access" },
                    onClick = onBrowseDiseasesClick
                )

                // Farming Tips
                MoreOptionItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconBg = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF1976D2),
                    title = when (language) { "te" -> "వ్యవసాయ చిట్కాలు"; "hi" -> "खेती के सुझाव"; else -> "Farming Tips" },
                    subtitle = when (language) { "te" -> "మంచి దిగుబడి కోసం"; "hi" -> "बेहतर पैदावार"; else -> "Grow Better" },
                    onClick = onFarmingTipsClick
                )

                // Chat with Agri AI Doctor
                MoreOptionItem(
                    icon = Icons.Default.Chat,
                    iconBg = Color(0xFFE8F5E9),
                    iconTint = FarmGreenPrimary,
                    title = when (language) { "te" -> "అగ్రి AI డాక్టర్‌తో చాట్ చేయండి"; "hi" -> "एग्रीकेयर AI डॉक्टर से चैट"; else -> "Chat with Agri AI Doctor" },
                    subtitle = when (language) { "te" -> "24/7 ఆఫ్లైన్ పంట వైద్య సలహా"; "hi" -> "24/7 ऑफलाइन फसल सलाह"; else -> "24/7 Offline Crop Advisory" },
                    onClick = onChatbotClick
                )

                // Help & Support
                MoreOptionItem(
                    icon = Icons.Default.HelpOutline,
                    iconBg = Color(0xFFEDE7F6),
                    iconTint = Color(0xFF512DA8),
                    title = when (language) { "te" -> "సహాయం & మద్దతు"; "hi" -> "सहायता और सपोर्ट"; else -> "Help & Support" },
                    subtitle = when (language) { "te" -> "KVK శాస్త్రవేత్తల హెల్ప్‌లైన్"; "hi" -> "कृषि वैज्ञानिक मदद"; else -> "KVK Agricultural Helpline" },
                    onClick = { /* Help */ }
                )

                // Share App via WhatsApp (Requested by user)
                MoreOptionItem(
                    icon = Icons.Default.Share,
                    iconBg = Color(0xFFE8F8F5),
                    iconTint = Color(0xFF00897B),
                    title = when (language) { "te" -> "యాప్‌ను వాట్సాప్‌లో షేర్ చేయండి"; "hi" -> "व्हाट्सएप पर ऐप शेयर करें"; else -> "Share App (WhatsApp)" },
                    subtitle = when (language) { "te" -> "రైతు మిత్రులతో పంచుకోండి"; "hi" -> "किसान भाइयों को भेजें"; else -> "Share with friends & farmers" },
                    onClick = {
                        shareAppViaWhatsApp(context)
                    }
                )

                // Import Llama 3.2 Model Fallback (Rule T5)
                val hasLlamaModel = File(context.filesDir, "llama-3.2-1b-spinquant.pte").exists() || File("/data/local/tmp/llama-3.2-1b-spinquant.pte").exists()
                MoreOptionItem(
                    icon = Icons.Default.CloudDownload,
                    iconBg = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFE65100),
                    title = when (language) { "te" -> "AI మోడల్ దిగుమతి (.pte)"; "hi" -> "AI मॉडल आयात करें (.pte)"; else -> "Import AI Model (.pte)" },
                    subtitle = if (hasLlamaModel) {
                        when (language) { "te" -> "Llama 3.2 1B సిద్ధంగా ఉంది"; "hi" -> "Llama 3.2 1B तैयार है"; else -> "Llama 3.2 1B Installed" }
                    } else {
                        when (language) { "te" -> ".pte లేదా tokenizer.model ఎంచుకోండి"; "hi" -> ".pte या tokenizer.model चुनें"; else -> "Select .pte or tokenizer.model" }
                    },
                    onClick = {
                        importModelLauncher.launch(arrayOf("*/*"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Badge (Matching sample photo)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌱", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Together for Healthy Agriculture\nArm Bharat AI-SoC Challenge",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MoreOptionItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun shareAppViaWhatsApp(context: Context) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "🌾 Crop Doctor (AgriCare AI) - 100% Offline Crop Disease & Pest Diagnosis for Indian Farmers! Built for Arm Bharat AI-SoC Challenge.\nDownload APK: https://github.com/varun-gajula/crop-doctor-offline/releases"
            )
            type = "text/plain"
            setPackage("com.whatsapp")
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        // Fallback to generic share sheet if WhatsApp is not installed
        val genericIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "🌾 Crop Doctor (AgriCare AI) - 100% Offline Crop Disease & Pest Diagnosis for Indian Farmers!\nDownload APK: https://github.com/varun-gajula/crop-doctor-offline/releases"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(genericIntent, "Share Crop Doctor"))
    }
}

