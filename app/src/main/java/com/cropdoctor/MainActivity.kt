package com.cropdoctor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.unit.sp
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.ui.*
import com.cropdoctor.ui.theme.CropDoctorTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class AppScreen {
    WELCOME,
    HOME,
    CAMERA,
    RESULT,
    DISEASE_DETAILS,
    KNOWLEDGE_BASE,
    LANGUAGE_PICK,
    MORE_OPTIONS,
    FARMING_TIPS,
    CHATBOT,
    SCAN_HISTORY
}

class MainActivity : ComponentActivity() {

    private var currentAdviceJob: Job? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Graceful handling */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CropDoctorTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                var previousScreen by remember { mutableStateOf(AppScreen.HOME) }

                var selectedLanguage by remember { mutableStateOf("en") }

                var classifierResult by remember { mutableStateOf<ClassifierResult?>(null) }
                var activeKbEntry by remember { mutableStateOf<KbEntry?>(null) }
                var selectedDetailEntry by remember { mutableStateOf<KbEntry?>(null) }

                var isProcessingImage by remember { mutableStateOf(false) }
                var showNonLeafDialog by remember { mutableStateOf(false) }
                var advisoryText by remember { mutableStateOf("") }
                var isGeneratingAdvice by remember { mutableStateOf(false) }
                var capturedLeafBitmap by remember { mutableStateOf<Bitmap?>(null) }

                val app = application as CropDoctorApplication

                fun navigateTo(screen: AppScreen) {
                    previousScreen = currentScreen
                    currentScreen = screen
                }

                // Handle system back button smoothly
                BackHandler(enabled = currentScreen != AppScreen.HOME) {
                    if (currentScreen == AppScreen.RESULT || currentScreen == AppScreen.CAMERA) {
                        currentScreen = AppScreen.HOME
                    } else {
                        currentScreen = previousScreen
                    }
                }

                // Dialog when non-plant object is scanned
                if (showNonLeafDialog) {
                    val dialogTitle = when (selectedLanguage) {
                        "te" -> "⚠️ పంట ఆకు గుర్తించబడలేదు"
                        "hi" -> "⚠️ फसल की पत्ती नहीं मिली"
                        else -> "⚠️ No Crop Leaf Detected"
                    }
                    val dialogMessage = when (selectedLanguage) {
                        "te" -> "స్కాన్ చేసిన ఫోటోలో పంట ఆకు కనిపించలేదు.\n\nదయచేసి వరి, పత్తి, మిరప, టమోటా లేదా మొక్కజొన్న ఆకుపై కెమెరాను స్పష్టంగా ఉంచి మళ్లీ ప్రయత్నించండి."
                        "hi" -> "स्कैन की गई तस्वीर में कोई फसल की पत्ती नहीं मिली।\n\nकृपया धान, कपास, मिर्च, टमाटर या मक्का की पत्ती पर ध्यान केंद्रित करके दोबारा प्रयास करें।"
                        else -> "The scanned photo does not appear to be a crop leaf.\n\nPlease position the camera clearly over a crop leaf (Rice, Cotton, Chilli, Tomato, or Maize) and try again."
                    }
                    val btnText = when (selectedLanguage) {
                        "te" -> "మళ్ళీ ప్రయత్నించండి"
                        "hi" -> "पुनः प्रयास करें"
                        else -> "Try Again"
                    }

                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showNonLeafDialog = false },
                        title = {
                            androidx.compose.material3.Text(
                                text = dialogTitle,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFFD32F2F)
                            )
                        },
                        text = {
                            androidx.compose.material3.Text(
                                text = dialogMessage,
                                fontSize = 15.sp
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = { showNonLeafDialog = false },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = com.cropdoctor.ui.theme.FarmGreenPrimary
                                )
                            ) {
                                androidx.compose.material3.Text(btnText)
                            }
                        }
                    )
                }

                when (currentScreen) {
                    AppScreen.WELCOME -> {
                        WelcomeScreen(
                            language = selectedLanguage,
                            onGetStarted = { navigateTo(AppScreen.HOME) },
                            onAboutClick = { navigateTo(AppScreen.MORE_OPTIONS) }
                        )
                    }

                    AppScreen.HOME -> {
                        HomeScreen(
                            language = selectedLanguage,
                            onScanClick = { navigateTo(AppScreen.CAMERA) },
                            onBrowseDiseasesClick = { navigateTo(AppScreen.KNOWLEDGE_BASE) },
                            onFarmingTipsClick = { navigateTo(AppScreen.FARMING_TIPS) },
                            onChangeLanguageClick = { navigateTo(AppScreen.LANGUAGE_PICK) },
                            onMoreOptionsClick = { navigateTo(AppScreen.MORE_OPTIONS) },
                            onChatbotClick = { navigateTo(AppScreen.CHATBOT) }
                        )
                    }

                    AppScreen.CAMERA -> {
                        MainCameraScreen(
                            language = selectedLanguage,
                            isProcessing = isProcessingImage,
                            onImageCaptured = { bitmap, cropFilter ->
                                isProcessingImage = true
                                capturedLeafBitmap = bitmap
                                lifecycleScope.launch {
                                    try {
                                        val result = app.classifier.classify(bitmap, cropFilter)

                                        // Gatekeeper: Check if object is a valid crop leaf
                                        if (result.classId == "not_a_leaf") {
                                            showNonLeafDialog = true
                                            return@launch
                                        }

                                        val kb = KnowledgeBase.getEntry(result.classId)
                                            ?: KnowledgeBase.getAllEntries().firstOrNull()

                                        if (kb != null) {
                                            classifierResult = result
                                            activeKbEntry = kb
                                            navigateTo(AppScreen.RESULT)

                                            triggerAdvisoryStream(
                                                app = app,
                                                result = result,
                                                kbEntry = kb,
                                                language = selectedLanguage,
                                                onUpdate = { advisoryText = it },
                                                onStateChange = { isGeneratingAdvice = it }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isProcessingImage = false
                                    }
                                }
                            },
                            onBackClick = { navigateTo(AppScreen.HOME) }
                        )
                    }

                    AppScreen.RESULT -> {
                        if (classifierResult != null && activeKbEntry != null) {
                            ResultScreen(
                                result = classifierResult!!,
                                kbEntry = activeKbEntry!!,
                                language = selectedLanguage,
                                advisoryText = advisoryText,
                                isGeneratingAdvice = isGeneratingAdvice,
                                capturedBitmap = capturedLeafBitmap,
                                onBackClick = { navigateTo(AppScreen.HOME) },
                                onChangeLanguageClick = { navigateTo(AppScreen.LANGUAGE_PICK) },
                                onViewMoreDetails = {
                                    selectedDetailEntry = activeKbEntry
                                    navigateTo(AppScreen.DISEASE_DETAILS)
                                },
                                onChatbotClick = {
                                    navigateTo(AppScreen.CHATBOT)
                                }
                            )
                        } else {
                            navigateTo(AppScreen.HOME)
                        }
                    }

                    AppScreen.DISEASE_DETAILS -> {
                        val entry = selectedDetailEntry ?: activeKbEntry ?: KnowledgeBase.getAllEntries().first()
                        DiseaseDetailsScreen(
                            kbEntry = entry,
                            language = selectedLanguage,
                            onBackClick = { navigateTo(previousScreen) },
                            onBackToHome = { navigateTo(AppScreen.HOME) }
                        )
                    }

                    AppScreen.KNOWLEDGE_BASE -> {
                        KnowledgeBaseScreen(
                            language = selectedLanguage,
                            onBackClick = { navigateTo(AppScreen.HOME) },
                            onDiseaseClick = { entry ->
                                selectedDetailEntry = entry
                                navigateTo(AppScreen.DISEASE_DETAILS)
                            }
                        )
                    }

                    AppScreen.LANGUAGE_PICK -> {
                        LanguagePickScreen(
                            currentLanguage = selectedLanguage,
                            onLanguageSelected = { newLang ->
                                selectedLanguage = newLang
                                // If previous was Result, regenerate streaming advice in new language
                                if (previousScreen == AppScreen.RESULT && classifierResult != null && activeKbEntry != null) {
                                    triggerAdvisoryStream(
                                        app = app,
                                        result = classifierResult!!,
                                        kbEntry = activeKbEntry!!,
                                        language = newLang,
                                        onUpdate = { advisoryText = it },
                                        onStateChange = { isGeneratingAdvice = it }
                                    )
                                }
                                currentScreen = previousScreen
                            },
                            onBackClick = { currentScreen = previousScreen }
                        )
                    }

                    AppScreen.MORE_OPTIONS -> {
                        MoreOptionsScreen(
                            language = selectedLanguage,
                            onBackClick = { navigateTo(AppScreen.HOME) },
                            onFarmingTipsClick = { navigateTo(AppScreen.FARMING_TIPS) },
                            onBrowseDiseasesClick = { navigateTo(AppScreen.KNOWLEDGE_BASE) },
                            onChatbotClick = { navigateTo(AppScreen.CHATBOT) },
                            onScanHistoryClick = { navigateTo(AppScreen.SCAN_HISTORY) }
                        )
                    }

                    AppScreen.FARMING_TIPS -> {
                        FarmingTipsScreen(
                            language = selectedLanguage,
                            onBackClick = { navigateTo(AppScreen.HOME) }
                        )
                    }

                    AppScreen.CHATBOT -> {
                        ChatbotScreen(
                            currentKbEntry = activeKbEntry,
                            language = selectedLanguage,
                            onBackClick = { navigateTo(previousScreen) }
                        )
                    }

                    AppScreen.SCAN_HISTORY -> {
                        ScanHistoryScreen(
                            language = selectedLanguage,
                            onBackClick = { navigateTo(AppScreen.HOME) },
                            onSelectRecord = { record ->
                                val kb = KnowledgeBase.getEntry(record.classId)
                                if (kb != null) {
                                    selectedDetailEntry = kb
                                    navigateTo(AppScreen.DISEASE_DETAILS)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerAdvisoryStream(
        app: CropDoctorApplication,
        result: ClassifierResult,
        kbEntry: KbEntry,
        language: String,
        onUpdate: (String) -> Unit,
        onStateChange: (Boolean) -> Unit
    ) {
        currentAdviceJob?.cancel()
        onUpdate("")
        onStateChange(true)

        currentAdviceJob = lifecycleScope.launch {
            val sb = StringBuilder()
            try {
                app.advisor.generateAdvice(
                    classId = result.classId,
                    severityBand = result.severityBand,
                    certainty = result.certaintyBand.name,
                    language = language,
                    kbEntry = kbEntry
                ) { token ->
                    sb.append(token)
                    onUpdate(sb.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onStateChange(false)
            }
        }
    }
}
