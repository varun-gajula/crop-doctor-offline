package com.cropdoctor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cropdoctor.advisor.ExecuTorchLlamaAdvisor
import com.cropdoctor.data.UserPreferences
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.knowledge.LocalizedText
import com.cropdoctor.knowledge.TreatmentPlan
import com.cropdoctor.ui.*
import com.cropdoctor.ui.theme.CropDoctorTheme
import com.cropdoctor.ui.theme.FarmGreenPrimary
import com.cropdoctor.vision.ImageAnalysisMetadata
import com.cropdoctor.vision.LeafValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import java.util.Locale
import com.cropdoctor.vision.ImagePreprocessor
import com.cropdoctor.vision.ExecuTorchClassifier

enum class AppScreen {
    FIRST_LAUNCH,
    WELCOME,
    HOME,
    CROP_SELECTION,
    CAMERA,
    RESULT,
    SAVE_CASE_REPORT,
    DISEASE_DETAILS,
    KNOWLEDGE_BASE,
    LANGUAGE_PICK,
    MORE_OPTIONS,
    FARMING_TIPS,
    CHATBOT,
    SCAN_HISTORY
}

class MainActivity : ComponentActivity() {

    private var currentDiagnosisJob: Job? = null
    private var currentAdviceJob: Job? = null
    private val currentRequestId = AtomicLong(0L)
    private var onBenchmarkAction: ((String) -> Unit)? = null
    private var onBenchmarkIntent: ((Intent) -> Unit)? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Graceful handling */ }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.getStringExtra("benchmark_action")
        Log.i("MainActivityBenchmark", "onNewIntent received action: $action")
        action?.let { onBenchmarkAction?.invoke(it) }
        onBenchmarkIntent?.invoke(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val userPrefs = UserPreferences(this)

        setContent {
            var selectedLanguage by remember { mutableStateOf(userPrefs.selectedLanguage) }
            var selectedRegion by remember { mutableStateOf(userPrefs.selectedRegion) }
            val locale = remember(selectedLanguage) { Locale(selectedLanguage) }
            val baseConfig = LocalConfiguration.current
            val configuration = remember(selectedLanguage, baseConfig) {
                Configuration(baseConfig).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val localizedContext = remember(selectedLanguage) {
                createConfigurationContext(configuration)
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                CropDoctorTheme {
                    val initialScreen = if (userPrefs.isFirstLaunch) AppScreen.FIRST_LAUNCH else AppScreen.HOME
                    var currentScreen by remember { mutableStateOf(initialScreen) }
                    var previousScreen by remember { mutableStateOf(initialScreen) }

                    var selectedCrop by remember { mutableStateOf("rice") }
                    var classifierResult by remember { mutableStateOf<ClassifierResult?>(null) }
                    var activeKbEntry by remember { mutableStateOf<KbEntry?>(null) }
                    var selectedDetailEntry by remember { mutableStateOf<KbEntry?>(null) }

                    var isProcessingImage by remember { mutableStateOf(false) }
                    var showSoftCropWarningDialog by remember { mutableStateOf(false) }
                    var pendingCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    var pendingCropFilter by remember { mutableStateOf<String?>(null) }
                    var pendingMetadata by remember { mutableStateOf<ImageAnalysisMetadata?>(null) }

                    var advisoryText by remember { mutableStateOf("") }
                    var isGeneratingAdvice by remember { mutableStateOf(false) }
                    var capturedLeafBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    var currentFieldSpread by remember { mutableStateOf<com.cropdoctor.domain.RulesEngine.FieldSpread?>(null) }

                    val app = application as CropDoctorApplication

                    fun navigateTo(screen: AppScreen) {
                        if (currentScreen == AppScreen.CAMERA && screen != AppScreen.RESULT) {
                            currentRequestId.incrementAndGet()
                            currentDiagnosisJob?.cancel()
                            currentAdviceJob?.cancel()
                            isProcessingImage = false
                        }
                        previousScreen = currentScreen
                        currentScreen = screen
                    }

                    fun switchLanguage(newLang: String) {
                        selectedLanguage = newLang
                        userPrefs.selectedLanguage = newLang
                        if (currentScreen == AppScreen.RESULT && classifierResult != null && activeKbEntry != null) {
                            triggerAdvisoryStream(
                                app = app,
                                result = classifierResult!!,
                                kbEntry = activeKbEntry!!,
                                language = newLang,
                                fieldSpread = currentFieldSpread,
                                region = selectedRegion,
                                crop = classifierResult?.selectedCrop ?: selectedCrop,
                                onUpdate = { advisoryText = it },
                                onStateChange = { isGeneratingAdvice = it }
                            )
                        }
                    }

                    fun switchRegion(newRegion: String) {
                        selectedRegion = newRegion
                        userPrefs.selectedRegion = newRegion
                        if (currentScreen == AppScreen.RESULT && classifierResult != null && activeKbEntry != null) {
                            triggerAdvisoryStream(
                                app = app,
                                result = classifierResult!!,
                                kbEntry = activeKbEntry!!,
                                language = selectedLanguage,
                                fieldSpread = currentFieldSpread,
                                region = newRegion,
                                crop = classifierResult?.selectedCrop ?: selectedCrop,
                                onUpdate = { advisoryText = it },
                                onStateChange = { isGeneratingAdvice = it }
                            )
                        }
                    }

                    // Handle system back button smoothly
                    BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.FIRST_LAUNCH) {
                        if (currentScreen == AppScreen.RESULT) {
                            currentScreen = AppScreen.HOME
                        } else if (currentScreen == AppScreen.CAMERA) {
                            currentScreen = AppScreen.CROP_SELECTION
                        } else if (currentScreen == AppScreen.CROP_SELECTION) {
                            currentScreen = AppScreen.HOME
                        } else if (currentScreen == AppScreen.SAVE_CASE_REPORT) {
                            currentScreen = AppScreen.RESULT
                        } else {
                            currentScreen = previousScreen
                        }
                    }

                    fun processImageBytes(
                        bytes: ByteArray,
                        source: String,
                        cropFilter: String?,
                        autoStartAdvisor: Boolean = true,
                        skipSoftWarning: Boolean = false
                    ) {
                        val thisRequestId = currentRequestId.incrementAndGet()
                        currentDiagnosisJob?.cancel()
                        currentAdviceJob?.cancel()
                        isProcessingImage = true

                        currentDiagnosisJob = lifecycleScope.launch(Dispatchers.Default) {
                            try {
                                val reusableBuf = (app.classifier as? ExecuTorchClassifier)?.getReusableBuffer()
                                val preprocessed = ImagePreprocessor.prepare(bytes, reusableBuf)

                                if (thisRequestId != currentRequestId.get() || !isActive) {
                                    preprocessed.bitmap512.recycle()
                                    return@launch
                                }

                                val meta = ImageAnalysisMetadata(
                                    source = source,
                                    origWidth = preprocessed.origWidth,
                                    origHeight = preprocessed.origHeight,
                                    exifOrientation = preprocessed.exifRotationDegrees,
                                    leafValidatorPass = preprocessed.leafValidation.isLikelyCrop,
                                    leafValidatorFraction = preprocessed.leafValidation.plantPixelFraction,
                                    leafValidatorReason = preprocessed.leafValidation.reason
                                )

                                val effectiveCropFilter = cropFilter ?: selectedCrop

                                if (!skipSoftWarning && !preprocessed.leafValidation.isLikelyCrop) {
                                    withContext(Dispatchers.Main) {
                                        if (thisRequestId != currentRequestId.get()) {
                                            preprocessed.bitmap512.recycle()
                                            return@withContext
                                        }
                                        pendingCapturedBitmap = preprocessed.bitmap512
                                        pendingCropFilter = effectiveCropFilter
                                        pendingMetadata = meta
                                        showSoftCropWarningDialog = true
                                        isProcessingImage = false
                                    }
                                    return@launch
                                }

                                val result = app.classifier.classify(preprocessed, effectiveCropFilter, meta)

                                if (thisRequestId != currentRequestId.get() || !isActive) {
                                    preprocessed.bitmap512.recycle()
                                    return@launch
                                }

                                val kb = KnowledgeBase.getEntry(result.classId)
                                    ?: if (result.classId != "unknown") {
                                        KbEntry(
                                            classId = result.classId,
                                            index = -1,
                                            crop = result.classId.substringBefore("_"),
                                            type = if (result.classId.endsWith("_healthy")) "healthy" else "disease",
                                            pathogen = "Awaiting expert verification",
                                            names = LocalizedText(
                                                en = result.displayName,
                                                te = result.displayName,
                                                hi = result.displayName
                                            ),
                                            referralThreshold = "MILD",
                                            immediateStep = LocalizedText(
                                                en = "Advice for this problem is not available yet. Ask an extension officer",
                                                te = "ఈ సమస్యకు సలహా ఇంకా అందుబాటులో లేదు. వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి.",
                                                hi = "इस समस्या के लिए सलाह अभी उपलब्ध नहीं है। कृषि विस्तार अधिकारी से संपर्क करें।"
                                            ),
                                            explainText = LocalizedText(
                                                en = "Advice for this problem is not available yet. Ask an extension officer or visit your nearest Krishi Vigyan Kendra.",
                                                te = "ఈ సమస్యకు సలహా ఇంకా అందుబాటులో లేదు. దయచేసి వ్యవసాయ విస్తరణ అధికారిని లేదా కృషి విజ్ఞాన కేంద్రాన్ని సంప్రదించండి.",
                                                hi = "इस समस्या के लिए सलाह अभी उपलब्ध नहीं है। कृपया कृषि विस्तार अधिकारी या नजदीकी कृषि विज्ञान केंद्र से संपर्क करें।"
                                            ),
                                            treatment = TreatmentPlan()
                                        )
                                    } else {
                                        KbEntry(
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
                                    }

                                withContext(Dispatchers.Main) {
                                    if (thisRequestId != currentRequestId.get()) {
                                        preprocessed.bitmap512.recycle()
                                        return@withContext
                                    }
                                    if (capturedLeafBitmap != null && capturedLeafBitmap != preprocessed.bitmap512 && !capturedLeafBitmap!!.isRecycled) {
                                        capturedLeafBitmap?.recycle()
                                    }
                                    capturedLeafBitmap = preprocessed.bitmap512
                                    classifierResult = result
                                    activeKbEntry = kb
                                    isProcessingImage = false
                                    navigateTo(AppScreen.RESULT)

                                    if (autoStartAdvisor) {
                                        triggerAdvisoryStream(
                                            app = app,
                                            result = result,
                                            kbEntry = kb,
                                            language = selectedLanguage,
                                            crop = result.selectedCrop ?: effectiveCropFilter,
                                            onUpdate = { advisoryText = it },
                                            onStateChange = { isGeneratingAdvice = it }
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                if (e !is CancellationException) {
                                    Log.e("MainActivity", "Diagnosis error", e)
                                }
                            } finally {
                                if (thisRequestId == currentRequestId.get()) {
                                    withContext(Dispatchers.Main) {
                                        isProcessingImage = false
                                    }
                                }
                            }
                        }
                    }

                    fun processDiagnosis(
                        bitmap: Bitmap,
                        cropFilter: String?,
                        autoStartAdvisor: Boolean = true,
                        metadata: ImageAnalysisMetadata? = null
                    ) {
                        val thisRequestId = currentRequestId.incrementAndGet()
                        currentDiagnosisJob?.cancel()
                        currentAdviceJob?.cancel()
                        isProcessingImage = true

                        currentDiagnosisJob = lifecycleScope.launch(Dispatchers.Default) {
                            try {
                                val effectiveCropFilter = cropFilter ?: selectedCrop
                                val result = app.classifier.classify(bitmap, effectiveCropFilter, metadata)

                                if (thisRequestId != currentRequestId.get() || !isActive) {
                                    return@launch
                                }

                                val kb = KnowledgeBase.getEntry(result.classId)
                                    ?: if (result.classId != "unknown") {
                                        KbEntry(
                                            classId = result.classId,
                                            index = -1,
                                            crop = result.classId.substringBefore("_"),
                                            type = if (result.classId.endsWith("_healthy")) "healthy" else "disease",
                                            pathogen = "Awaiting expert verification",
                                            names = LocalizedText(
                                                en = result.displayName,
                                                te = result.displayName,
                                                hi = result.displayName
                                            ),
                                            referralThreshold = "MILD",
                                            immediateStep = LocalizedText(
                                                en = "Advice for this problem is not available yet. Ask an extension officer",
                                                te = "ఈ సమస్యకు సలహా ఇంకా అందుబాటులో లేదు. వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి.",
                                                hi = "इस समस्या के लिए सलाह अभी उपलब्ध नहीं है। कृषि विस्तार अधिकारी से संपर्क करें।"
                                            ),
                                            explainText = LocalizedText(
                                                en = "Advice for this problem is not available yet. Ask an extension officer or visit your nearest Krishi Vigyan Kendra.",
                                                te = "ఈ సమస్యకు సలహా ఇంకా అందుబాటులో లేదు. దయచేసి వ్యవసాయ విస్తరణ అధికారిని లేదా కృషి విజ్ఞాన కేంద్రాన్ని సంప్రదించండి.",
                                                hi = "इस समस्या के लिए सलाह अभी उपलब्ध नहीं है। कृपया कृषि विस्तार अधिकारी या नजदीकी कृषि विज्ञान केंद्र से संपर्क करें।"
                                            ),
                                            treatment = TreatmentPlan()
                                        )
                                    } else {
                                        KbEntry(
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
                                    }

                                withContext(Dispatchers.Main) {
                                    if (thisRequestId != currentRequestId.get()) return@withContext
                                    if (capturedLeafBitmap != null && capturedLeafBitmap != bitmap && !capturedLeafBitmap!!.isRecycled) {
                                        capturedLeafBitmap?.recycle()
                                    }
                                    capturedLeafBitmap = bitmap
                                    classifierResult = result
                                    activeKbEntry = kb
                                    isProcessingImage = false
                                    navigateTo(AppScreen.RESULT)

                                    if (autoStartAdvisor) {
                                        triggerAdvisoryStream(
                                            app = app,
                                            result = result,
                                            kbEntry = kb,
                                            language = selectedLanguage,
                                            crop = result.selectedCrop ?: effectiveCropFilter,
                                            onUpdate = { advisoryText = it },
                                            onStateChange = { isGeneratingAdvice = it }
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                if (e !is CancellationException) {
                                    Log.e("MainActivity", "Diagnosis error", e)
                                }
                            } finally {
                                if (thisRequestId == currentRequestId.get()) {
                                    withContext(Dispatchers.Main) {
                                        isProcessingImage = false
                                    }
                                }
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        onBenchmarkIntent = { intent ->
                            val action = intent.getStringExtra("benchmark_action") ?: ""
                            if (action == "diagnose_file") {
                                val filePath = intent.getStringExtra("file_path")
                                val source = intent.getStringExtra("image_source") ?: "gallery"
                                val cropHint = intent.getStringExtra("crop_hint") ?: "auto"
                                if (filePath != null) {
                                    try {
                                        val file = File(filePath)
                                        if (file.exists()) {
                                            val bytes = file.readBytes()
                                            processImageBytes(bytes, source, cropHint, autoStartAdvisor = false, skipSoftWarning = true)
                                        } else {
                                            Log.e("MainActivityBenchmark", "diagnose_file: file not found: $filePath")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivityBenchmark", "diagnose_file error", e)
                                    }
                                }
                            }
                        }

                        onBenchmarkAction = { action ->
                            Log.i("MainActivityBenchmark", "Executing benchmark action: $action")
                            when (action) {
                                "vision" -> {
                                    val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                                    for (x in 0 until 512) {
                                        for (y in 0 until 512) {
                                            bmp.setPixel(x, y, android.graphics.Color.rgb(34, 139, 34))
                                        }
                                    }
                                    processDiagnosis(bmp, "tomato", autoStartAdvisor = false)
                                }
                                "load_llama" -> {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        (app.advisor as? ExecuTorchLlamaAdvisor)?.ensureLoadedForBenchmark()
                                        Log.i("MainActivityBenchmark", "Llama model loaded and idle")
                                    }
                                }
                                "stream_llama" -> {
                                    val kb = KnowledgeBase.getEntry("tomato_yellow_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "tomato_yellow_leaf_curl_virus",
                                        displayName = "Tomato Yellow Leaf Curl Virus",
                                        confidence = 0.88f,
                                        severityBand = SeverityBand.MODERATE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.22f,
                                        inferenceTimeMs = 85L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                    triggerAdvisoryStream(
                                        app = app,
                                        result = result,
                                        kbEntry = kb,
                                        language = "en",
                                        onUpdate = { advisoryText = it },
                                        onStateChange = { isGeneratingAdvice = it }
                                    )
                                }
                                "show_disease" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("tomato_yellow_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first { it.type == "disease" }
                                    val result = ClassifierResult(
                                        classId = "tomato_yellow_leaf_curl_virus",
                                        displayName = kb.names.forLanguage(selectedLanguage),
                                        confidence = 0.88f,
                                        severityBand = SeverityBand.MODERATE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.22f,
                                        inferenceTimeMs = 85L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_pest" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("maize_common_rust")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "maize_common_rust",
                                        displayName = kb.names.forLanguage(selectedLanguage),
                                        confidence = 0.91f,
                                        severityBand = SeverityBand.MODERATE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.18f,
                                        inferenceTimeMs = 82L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_healthy" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("tomato_healthy")
                                        ?: KnowledgeBase.getAllEntries().first { it.type == "healthy" }
                                    val result = ClassifierResult(
                                        classId = "tomato_healthy",
                                        displayName = kb.names.forLanguage(selectedLanguage),
                                        confidence = 0.94f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.02f,
                                        inferenceTimeMs = 80L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "lang_en" -> switchLanguage("en")
                                "lang_te" -> switchLanguage("te")
                                "lang_hi" -> switchLanguage("hi")
                                "show_crop_selection_en" -> {
                                    switchLanguage("en")
                                    navigateTo(AppScreen.CROP_SELECTION)
                                }
                                "show_crop_selection_te" -> {
                                    switchLanguage("te")
                                    navigateTo(AppScreen.CROP_SELECTION)
                                }
                                "show_crop_selection_hi" -> {
                                    switchLanguage("hi")
                                    navigateTo(AppScreen.CROP_SELECTION)
                                }
                                "region_telangana" -> switchRegion("Telangana")
                                "region_andhra" -> switchRegion("Andhra Pradesh")
                                "region_other" -> switchRegion("Other")
                                "show_mild_chilli" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("chilli_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "chilli_leaf_curl_virus",
                                        displayName = kb.names.forLanguage(selectedLanguage),
                                        confidence = 0.90f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.05f,
                                        inferenceTimeMs = 82L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_chilli" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("chilli_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "chilli_leaf_curl_virus",
                                        displayName = kb.names.forLanguage(selectedLanguage),
                                        confidence = 0.89f,
                                        severityBand = SeverityBand.MODERATE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.20f,
                                        inferenceTimeMs = 84L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_severe_chilli" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("chilli_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "chilli_leaf_curl_virus",
                                        displayName = kb.names.forLanguage(selectedLanguage),
                                        confidence = 0.95f,
                                        severityBand = SeverityBand.SEVERE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.55f,
                                        inferenceTimeMs = 84L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "release_llama" -> {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        (app.advisor as? ExecuTorchLlamaAdvisor)?.forceReleaseForBenchmark()
                                        Log.i("MainActivityBenchmark", "Llama model released")
                                    }
                                }
                            }
                        }
                        intent?.getStringExtra("benchmark_action")?.let { action ->
                            onBenchmarkAction?.invoke(action)
                        }
                        onDispose {
                            onBenchmarkAction = null
                        }
                    }

                    // Soft warning dialog for non-standard crop photos (never blocks, offers retake or proceed)
                    if (showSoftCropWarningDialog && pendingCapturedBitmap != null) {
                        AlertDialog(
                            onDismissRequest = {
                                showSoftCropWarningDialog = false
                                pendingCapturedBitmap = null
                            },
                            title = {
                                Text(
                                    text = "⚠️ " + stringResource(R.string.warn_not_crop_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.warn_not_crop_msg),
                                    fontSize = 15.sp
                                )
                            },
                            dismissButton = {
                                OutlinedButton(onClick = {
                                    showSoftCropWarningDialog = false
                                    pendingCapturedBitmap = null
                                }) {
                                    Text(stringResource(R.string.btn_take_again))
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showSoftCropWarningDialog = false
                                        val bmp = pendingCapturedBitmap
                                        val cf = pendingCropFilter
                                        val meta = pendingMetadata
                                        pendingCapturedBitmap = null
                                        pendingCropFilter = null
                                        pendingMetadata = null
                                        if (bmp != null) {
                                            processDiagnosis(bmp, cf, metadata = meta)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FarmGreenPrimary
                                    )
                                ) {
                                    Text(stringResource(R.string.btn_use_anyway))
                                }
                            }
                        )
                    }

                    when (currentScreen) {
                        AppScreen.FIRST_LAUNCH -> {
                            FirstLaunchScreen(
                                currentLanguage = selectedLanguage,
                                onLanguageChange = { newLang ->
                                    switchLanguage(newLang)
                                },
                                onComplete = { lang, region ->
                                    userPrefs.isFirstLaunch = false
                                    switchRegion(region)
                                    switchLanguage(lang)
                                    navigateTo(AppScreen.HOME)
                                }
                            )
                        }

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
                                onScanClick = { navigateTo(AppScreen.CROP_SELECTION) },
                                onBrowseDiseasesClick = { navigateTo(AppScreen.KNOWLEDGE_BASE) },
                                onFarmingTipsClick = { navigateTo(AppScreen.FARMING_TIPS) },
                                onChangeLanguageClick = { newLang -> switchLanguage(newLang) },
                                onMoreOptionsClick = { navigateTo(AppScreen.MORE_OPTIONS) },
                                onChatbotClick = { navigateTo(AppScreen.CHATBOT) }
                            )
                        }

                        AppScreen.CROP_SELECTION -> {
                            CropSelectionScreen(
                                language = selectedLanguage,
                                onCropSelected = { crop ->
                                    selectedCrop = crop
                                    navigateTo(AppScreen.CAMERA)
                                },
                                onBackClick = { navigateTo(AppScreen.HOME) },
                                onLanguageChange = { newLang -> switchLanguage(newLang) }
                            )
                        }

                        AppScreen.CAMERA -> {
                            MainCameraScreen(
                                language = selectedLanguage,
                                selectedCrop = selectedCrop,
                                isProcessing = isProcessingImage,
                                onImageBytesCaptured = { bytes, cropFilter, source ->
                                    val effectiveCropFilter = cropFilter ?: selectedCrop
                                    processImageBytes(bytes, source, effectiveCropFilter)
                                },
                                onImageCaptured = { bitmap, cropFilter ->
                                    val effectiveCropFilter = cropFilter ?: selectedCrop
                                    val validation = LeafValidator.validate(bitmap)
                                    val meta = ImageAnalysisMetadata(
                                        source = "camera",
                                        origWidth = bitmap.width,
                                        origHeight = bitmap.height,
                                        leafValidatorPass = validation.isLikelyCrop,
                                        leafValidatorFraction = validation.plantPixelFraction,
                                        leafValidatorReason = validation.reason
                                    )
                                    if (!validation.isLikelyCrop) {
                                        pendingCapturedBitmap = bitmap
                                        pendingCropFilter = effectiveCropFilter
                                        pendingMetadata = meta
                                        showSoftCropWarningDialog = true
                                    } else {
                                        processDiagnosis(bitmap, effectiveCropFilter, metadata = meta)
                                    }
                                },
                                onImageCapturedWithMetadata = { bitmap, cropFilter, meta ->
                                    val effectiveCropFilter = cropFilter ?: selectedCrop
                                    val validation = LeafValidator.validate(bitmap)
                                    val enrichedMeta = meta.copy(
                                        leafValidatorPass = validation.isLikelyCrop,
                                        leafValidatorFraction = validation.plantPixelFraction,
                                        leafValidatorReason = validation.reason
                                    )
                                    if (!validation.isLikelyCrop) {
                                        pendingCapturedBitmap = bitmap
                                        pendingCropFilter = effectiveCropFilter
                                        pendingMetadata = enrichedMeta
                                        showSoftCropWarningDialog = true
                                    } else {
                                        processDiagnosis(bitmap, effectiveCropFilter, metadata = enrichedMeta)
                                    }
                                },
                                onBackClick = { navigateTo(AppScreen.CROP_SELECTION) },
                                onChangeCropClick = { navigateTo(AppScreen.CROP_SELECTION) },
                                onChangeLanguageClick = { newLang -> switchLanguage(newLang) }
                            )
                        }

                        AppScreen.RESULT -> {
                            if (classifierResult != null && activeKbEntry != null) {
                                ResultScreen(
                                    result = classifierResult!!,
                                    kbEntry = activeKbEntry!!,
                                    language = selectedLanguage,
                                    region = selectedRegion,
                                    onRegionChange = { newRegion ->
                                        switchRegion(newRegion)
                                    },
                                    advisoryText = advisoryText,
                                    isGeneratingAdvice = isGeneratingAdvice,
                                    isFakeAdvisor = try { (application as CropDoctorApplication).advisor.isFake } catch (e: Exception) { true },
                                    isLowMemoryFallback = try { (application as CropDoctorApplication).advisor.isLowMemoryFallback } catch (e: Exception) { false },
                                    isOfflineGuideFallback = try { (application as CropDoctorApplication).advisor.isFallbackToOfflineGuide } catch (e: Exception) { false },
                                    capturedBitmap = capturedLeafBitmap,
                                    onBackClick = { navigateTo(AppScreen.HOME) },
                                    onChangeLanguageClick = { newLang -> switchLanguage(newLang) },
                                    onFieldSpreadChange = { spread ->
                                        currentFieldSpread = spread
                                        if (classifierResult != null && activeKbEntry != null) {
                                            triggerAdvisoryStream(
                                                app = app,
                                                result = classifierResult!!,
                                                kbEntry = activeKbEntry!!,
                                                language = selectedLanguage,
                                                fieldSpread = spread,
                                                region = selectedRegion,
                                                crop = classifierResult!!.selectedCrop ?: selectedCrop,
                                                onUpdate = { advisoryText = it },
                                                onStateChange = { isGeneratingAdvice = it }
                                            )
                                        }
                                    },
                                    onViewMoreDetails = {
                                        selectedDetailEntry = activeKbEntry
                                        navigateTo(AppScreen.DISEASE_DETAILS)
                                    },
                                    onChatbotClick = {
                                        navigateTo(AppScreen.CHATBOT)
                                    },
                                    onSaveCaseReport = {
                                        navigateTo(AppScreen.SAVE_CASE_REPORT)
                                    },
                                    onCancelAdvice = {
                                        currentAdviceJob?.cancel()
                                        (application as? CropDoctorApplication)?.advisor?.stop()
                                        isGeneratingAdvice = false
                                    }
                                )
                            } else {
                                navigateTo(AppScreen.HOME)
                            }
                        }

                        AppScreen.SAVE_CASE_REPORT -> {
                            if (classifierResult != null && activeKbEntry != null) {
                                SaveCaseReportScreen(
                                    result = classifierResult!!,
                                    kbEntry = activeKbEntry!!,
                                    language = selectedLanguage,
                                    region = selectedRegion,
                                    capturedBitmap = capturedLeafBitmap,
                                    onBackClick = { navigateTo(AppScreen.RESULT) },
                                    onViewSavedCases = { navigateTo(AppScreen.SCAN_HISTORY) }
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
                                    switchLanguage(newLang)
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
                                onSelectCase = { case ->
                                    val kb = KnowledgeBase.getEntry(case.classId)
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
    }

    private fun triggerAdvisoryStream(
        app: CropDoctorApplication,
        result: ClassifierResult,
        kbEntry: KbEntry,
        language: String,
        fieldSpread: com.cropdoctor.domain.RulesEngine.FieldSpread? = null,
        region: String = "telangana",
        crop: String? = null,
        onUpdate: (String) -> Unit,
        onStateChange: (Boolean) -> Unit
    ) {
        currentAdviceJob?.cancel()
        onUpdate("")
        onStateChange(true)

        val spreadStr = when (fieldSpread) {
            com.cropdoctor.domain.RulesEngine.FieldSpread.FEW_PLANTS -> "a few plants"
            com.cropdoctor.domain.RulesEngine.FieldSpread.MOST -> "most"
            else -> "some"
        }
        val spreadEnum = fieldSpread ?: com.cropdoctor.domain.RulesEngine.FieldSpread.SOME
        val urgency = com.cropdoctor.domain.RulesEngine.calculateUrgency(result.severityBand, spreadEnum)
        val effectiveCrop = crop ?: result.selectedCrop ?: kbEntry.crop

        currentAdviceJob = lifecycleScope.launch {
            val sb = StringBuilder()
            try {
                app.advisor.generateAdvice(
                    classId = result.classId,
                    severityBand = result.severityBand,
                    certainty = result.certaintyBand.name,
                    language = language,
                    kbEntry = kbEntry,
                    fieldSpread = spreadStr,
                    region = region,
                    urgency = urgency,
                    crop = effectiveCrop
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
