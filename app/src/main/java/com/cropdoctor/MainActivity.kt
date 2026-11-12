package com.cropdoctor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.knowledge.LocalizedText
import com.cropdoctor.knowledge.TreatmentPlan
import com.cropdoctor.ui.*
import com.cropdoctor.ui.theme.CropDoctorTheme
import com.cropdoctor.ui.theme.FarmGreenPrimary
import com.cropdoctor.vision.LeafValidator
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
                var showSoftCropWarningDialog by remember { mutableStateOf(false) }
                var pendingCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var pendingCropFilter by remember { mutableStateOf<String?>(null) }

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

                fun processDiagnosis(bitmap: Bitmap, cropFilter: String?) {
                    isProcessingImage = true
                    capturedLeafBitmap = bitmap
                    lifecycleScope.launch {
                        try {
                            val result = app.classifier.classify(bitmap, cropFilter)

                            val kb = KnowledgeBase.getEntry(result.classId)
                                ?: KbEntry(
                                    classId = "unknown",
                                    index = -1,
                                    crop = "crop",
                                    type = "unknown",
                                    pathogen = "Unknown / Unverified",
                                    names = LocalizedText(
                                        en = "Not sure - ask an extension officer",
                                        te = "ఖచ్చితంగా తెలియదు",
                                        hi = "अनिश्चित स्थिति"
                                    ),
                                    referralThreshold = "MILD",
                                    immediateStep = LocalizedText(
                                        en = "Consult an agricultural extension officer.",
                                        te = "వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి.",
                                        hi = "कृषि विस्तार अधिकारी से संपर्क करें।"
                                    ),
                                    explainText = LocalizedText(
                                        en = "The condition could not be identified with certainty. Please ask an agriculture extension officer or visit your nearest Krishi Vigyan Kendra.",
                                        te = "పరిస్థితిని ఖచ్చితంగా గుర్తించలేకపోయాము. దయచేసి వ్యవసాయ విస్తరణ అధికారిని లేదా కృషి విజ్ఞాన కేంద్రాన్ని సంప్రదించండి.",
                                        hi = "स्थिति की निश्चित पहचान नहीं हो सकी। कृपया कृषि विस्तार अधिकारी या नजदीकी कृषि विज्ञान केंद्र से संपर्क करें।"
                                    ),
                                    treatment = TreatmentPlan()
                                )

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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isProcessingImage = false
                        }
                    }
                }

                // Soft warning dialog for non-standard crop photos (never blocks, offers retake or proceed)
                if (showSoftCropWarningDialog && pendingCapturedBitmap != null) {
                    val dialogTitle = when (selectedLanguage) {
                        "te" -> "⚠️ ఇది పంట ఫోటో కాకపోవచ్చు"
                        "hi" -> "⚠️ यह फसल की तस्वीर नहीं लग रही है"
                        else -> "⚠️ This may not be a crop photo"
                    }
                    val dialogMessage = when (selectedLanguage) {
                        "te" -> "తీసిన ఫోటో పంట ఆకు, పండు లేదా పురుగులా స్పష్టంగా కనిపించడం లేదు. మీరు మళ్ళీ ఫోటో తీయవచ్చు లేదా ఈ ఫోటోతోనే విశ్లేషణను కొనసాగించవచ్చు."
                        "hi" -> "खींची गई तस्वीर स्पष्ट रूप से फसल की पत्ती, फल या कीट जैसी नहीं लग रही है। आप दोबारा तस्वीर ले सकते हैं या इसी तस्वीर के साथ आगे बढ़ सकते हैं।"
                        else -> "The captured photo does not clearly resemble a crop leaf, fruit, or pest. You can retake the photo or use this photo anyway."
                    }
                    val retakeBtnText = when (selectedLanguage) {
                        "te" -> "మళ్ళీ తీయండి"
                        "hi" -> "दोबारा लें"
                        else -> "Take again"
                    }
                    val proceedBtnText = when (selectedLanguage) {
                        "te" -> "ఇలాగే ఉపయోగించండి"
                        "hi" -> "फिर भी इस्तेमाल करें"
                        else -> "Use this photo anyway"
                    }

                    AlertDialog(
                        onDismissRequest = {
                            showSoftCropWarningDialog = false
                            pendingCapturedBitmap = null
                        },
                        title = {
                            Text(
                                text = dialogTitle,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        },
                        text = {
                            Text(
                                text = dialogMessage,
                                fontSize = 15.sp
                            )
                        },
                        dismissButton = {
                            OutlinedButton(onClick = {
                                showSoftCropWarningDialog = false
                                pendingCapturedBitmap = null
                            }) {
                                Text(retakeBtnText)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showSoftCropWarningDialog = false
                                    val bmp = pendingCapturedBitmap
                                    val cf = pendingCropFilter
                                    pendingCapturedBitmap = null
                                    if (bmp != null) {
                                        processDiagnosis(bmp, cf)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FarmGreenPrimary
                                )
                            ) {
                                Text(proceedBtnText)
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
                                val validation = LeafValidator.validate(bitmap)
                                if (!validation.isLikelyCrop) {
                                    pendingCapturedBitmap = bitmap
                                    pendingCropFilter = cropFilter
                                    showSoftCropWarningDialog = true
                                } else {
                                    processDiagnosis(bitmap, cropFilter)
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
