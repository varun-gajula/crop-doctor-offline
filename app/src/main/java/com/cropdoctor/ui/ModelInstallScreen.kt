package com.cropdoctor.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.CropDoctorApplication
import com.cropdoctor.R
import com.cropdoctor.advisor.ExecuTorchLlamaAdvisor
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary
import com.cropdoctor.voice.VoiceModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelInstallScreen(
    language: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isCopying by remember { mutableStateOf(false) }
    var copyStatusText by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    val app = context.applicationContext as? CropDoctorApplication
    val advisor = app?.advisor as? ExecuTorchLlamaAdvisor

    // Check files whenever refreshTrigger changes
    val modelFileInternal = remember(refreshTrigger) { File(context.filesDir, "llama-3.2-1b-spinquant.pte") }
    val tokenizerFileInternal = remember(refreshTrigger) { File(context.filesDir, "tokenizer.model") }

    val modelFileExternal = remember(refreshTrigger) {
        val ext = context.getExternalFilesDir(null)
        if (ext != null) File(ext, "llama-3.2-1b-spinquant.pte") else null
    }
    val tokenizerFileExternal = remember(refreshTrigger) {
        val ext = context.getExternalFilesDir(null)
        if (ext != null) File(ext, "tokenizer.model") else null
    }

    val modelFileTmp = remember(refreshTrigger) { File("/data/local/tmp/llama-3.2-1b-spinquant.pte") }
    val tokenizerFileTmp = remember(refreshTrigger) { File("/data/local/tmp/tokenizer.model") }

    val hasModel = (modelFileInternal.exists() && modelFileInternal.length() > 50_000_000L) ||
            (modelFileExternal?.exists() == true && modelFileExternal.length() > 50_000_000L) ||
            (modelFileTmp.exists() && modelFileTmp.canRead() && modelFileTmp.length() > 50_000_000L)

    val hasTokenizer = (tokenizerFileInternal.exists() && tokenizerFileInternal.length() > 50_000L) ||
            (tokenizerFileExternal?.exists() == true && tokenizerFileExternal.length() > 50_000L) ||
            (tokenizerFileTmp.exists() && tokenizerFileTmp.canRead() && tokenizerFileTmp.length() > 50_000L)

    val modelSizeMb = when {
        modelFileInternal.exists() && modelFileInternal.length() > 50_000_000L -> modelFileInternal.length() / (1024 * 1024)
        modelFileExternal?.exists() == true && modelFileExternal.length() > 50_000_000L -> modelFileExternal.length() / (1024 * 1024)
        modelFileTmp.exists() && modelFileTmp.canRead() && modelFileTmp.length() > 50_000_000L -> modelFileTmp.length() / (1024 * 1024)
        else -> 0L
    }

    val tokenizerSizeKb = when {
        tokenizerFileInternal.exists() && tokenizerFileInternal.length() > 50_000L -> tokenizerFileInternal.length() / 1024
        tokenizerFileExternal?.exists() == true && tokenizerFileExternal.length() > 50_000L -> tokenizerFileExternal.length() / 1024
        tokenizerFileTmp.exists() && tokenizerFileTmp.canRead() && tokenizerFileTmp.length() > 50_000L -> tokenizerFileTmp.length() / 1024
        else -> 0L
    }

    // Voice recognition models (Vosk)
    val hasVoiceEn = remember(refreshTrigger) { VoiceModelManager.isModelInstalled(context, "en") }
    val hasVoiceHi = remember(refreshTrigger) { VoiceModelManager.isModelInstalled(context, "hi") }
    val hasVoiceTe = remember(refreshTrigger) { VoiceModelManager.isModelInstalled(context, "te") }
    val voiceEnSizeMb = remember(refreshTrigger) { VoiceModelManager.getInstalledModelSizeMb(context, "en") }
    val voiceHiSizeMb = remember(refreshTrigger) { VoiceModelManager.getInstalledModelSizeMb(context, "hi") }
    val voiceTeSizeMb = remember(refreshTrigger) { VoiceModelManager.getInstalledModelSizeMb(context, "te") }

    var pendingVoiceLang by remember { mutableStateOf("en") }

    val voiceModelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                isCopying = true
                copyStatusText = "Extracting Vosk speech model zip for $pendingVoiceLang..."
                try {
                    val ok = VoiceModelManager.installModelFromZip(context, uri, pendingVoiceLang)
                    withContext(Dispatchers.Main) {
                        if (ok) {
                            Toast.makeText(context, "Voice model for $pendingVoiceLang installed successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to extract voice model for $pendingVoiceLang", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Voice install error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isCopying = false
                    copyStatusText = ""
                    refreshTrigger++
                }
            }
        }
    }

    // SAF file picker for .pte
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                isCopying = true
                copyStatusText = "Copying Llama 3.2 1B model file (1.13 GB)..."
                try {
                    val destFile = File(context.filesDir, "llama-3.2-1b-spinquant.pte")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            val buf = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (input.read(buf).also { bytesRead = it } != -1) {
                                output.write(buf, 0, bytesRead)
                            }
                        }
                    }
                    if (destFile.length() < 100_000_000L) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Warning: Model file seems too small (${destFile.length()} bytes)", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        advisor?.findModelFiles()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Llama 3.2 1B Model Installed Successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isCopying = false
                    copyStatusText = ""
                    refreshTrigger++
                }
            }
        }
    }

    // SAF file picker for tokenizer.model
    val tokenizerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                isCopying = true
                copyStatusText = "Copying tokenizer file..."
                try {
                    val destFile = File(context.filesDir, "tokenizer.model")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (destFile.length() < 50_000L) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Warning: Tokenizer file seems too small (${destFile.length()} bytes)", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        advisor?.findModelFiles()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Tokenizer Installed Successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isCopying = false
                    copyStatusText = ""
                    refreshTrigger++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.model_install_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAF7))
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Explanatory card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline AI Advisory",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.model_install_desc),
                        fontSize = 14.sp,
                        color = Color(0xFF424242),
                        lineHeight = 20.sp
                    )
                }
            }

            // Green status banner when models are already present
            if (hasModel && hasTokenizer) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.dp, Color(0xFF81C784))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "All AI Models Installed & Ready",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Llama 3.2 1B & Tokenizer are active. No further installation required.",
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            if (isCopying) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = FarmGreenPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = copyStatusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = FarmGreenDark
                        )
                    }
                }
            }

            // Model file card (.pte)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.model_file_label),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        if (hasModel) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stringResource(R.string.status_installed)} ($modelSizeMb MB)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.status_not_installed),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File: llama-3.2-1b-spinquant.pte (~1.13 GB)\nTarget: ${context.filesDir.absolutePath}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF616161)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    if (hasModel) {
                        OutlinedButton(
                            onClick = { modelPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCopying
                        ) {
                            Text(
                                text = "Reinstall / Replace Model (.pte)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Button(
                            onClick = { modelPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCopying
                        ) {
                            Text(
                                text = stringResource(R.string.btn_select_model_file),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Tokenizer file card (.model)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.tokenizer_file_label),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        if (hasTokenizer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stringResource(R.string.status_installed)} ($tokenizerSizeKb KB)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.status_not_installed),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File: tokenizer.model (~2.2 MB)\nTarget: ${context.filesDir.absolutePath}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF616161)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    if (hasTokenizer) {
                        OutlinedButton(
                            onClick = { tokenizerPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCopying
                        ) {
                            Text(
                                text = "Reinstall / Replace Tokenizer (.model)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Button(
                            onClick = { tokenizerPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCopying
                        ) {
                            Text(
                                text = stringResource(R.string.btn_select_tokenizer_file),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Remove files button (for verifying offline guide fallback per Task A-REAL step 6)
            if (modelFileInternal.exists() || tokenizerFileInternal.exists()) {
                OutlinedButton(
                    onClick = {
                        if (modelFileInternal.exists()) modelFileInternal.delete()
                        if (tokenizerFileInternal.exists()) tokenizerFileInternal.delete()
                        advisor?.findModelFiles()
                        Toast.makeText(context, "Model files removed from internal storage", Toast.LENGTH_SHORT).show()
                        refreshTrigger++
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_remove_model_files),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Voice Recognition Models (Vosk Offline - Task VOICE)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎙️ Offline Speech Models (Vosk)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apache 2.0 open-source offline speech models for voice questions in Chat.",
                        fontSize = 12.sp,
                        color = Color(0xFF616161)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // English
                    VoiceModelRow(
                        name = "Indian English (vosk-model-small-en-in-0.4)",
                        isInstalled = hasVoiceEn,
                        sizeMb = voiceEnSizeMb,
                        onSelect = {
                            pendingVoiceLang = "en"
                            voiceModelPickerLauncher.launch(arrayOf("*/*"))
                        },
                        isCopying = isCopying
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Hindi
                    VoiceModelRow(
                        name = "हिन्दी (vosk-model-small-hi-0.22)",
                        isInstalled = hasVoiceHi,
                        sizeMb = voiceHiSizeMb,
                        onSelect = {
                            pendingVoiceLang = "hi"
                            voiceModelPickerLauncher.launch(arrayOf("*/*"))
                        },
                        isCopying = isCopying
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Telugu
                    VoiceModelRow(
                        name = "తెలుగు (vosk-model-small-te-0.42)",
                        isInstalled = hasVoiceTe,
                        sizeMb = voiceTeSizeMb,
                        onSelect = {
                            pendingVoiceLang = "te"
                            voiceModelPickerLauncher.launch(arrayOf("*/*"))
                        },
                        isCopying = isCopying
                    )
                }
            }

            // ADB push instructions card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Developer / ADB Push Option",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val extPath = context.getExternalFilesDir(null)?.absolutePath ?: "/sdcard/Android/data/com.cropdoctor/files"
                    Text(
                        text = "You can also push directly via USB cable (no root required):\n\n" +
                                "adb push Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte $extPath/llama-3.2-1b-spinquant.pte\n\n" +
                                "adb push tokenizer.model $extPath/tokenizer.model",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VoiceModelRow(
    name: String,
    isInstalled: Boolean,
    sizeMb: Long,
    onSelect: () -> Unit,
    isCopying: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
            Text(
                text = if (isInstalled) "Installed (~$sizeMb MB)" else "Not Installed",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInstalled) Color(0xFF2E7D32) else Color(0xFFD97706)
            )
        }
        OutlinedButton(
            onClick = onSelect,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            enabled = !isCopying
        ) {
            Text(text = if (isInstalled) "Reinstall (.zip)" else "Install (.zip)", fontSize = 12.sp)
        }
    }
}
