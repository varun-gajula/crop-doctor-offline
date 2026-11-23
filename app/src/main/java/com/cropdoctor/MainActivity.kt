package com.cropdoctor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import android.os.Bundle
import android.os.Debug
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
import com.cropdoctor.chat.ChatEngine
import com.cropdoctor.data.UserPreferences
import com.cropdoctor.voice.VoskVoiceRecognizer
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.DiagnosisOutcome
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong
import java.util.Locale
import com.cropdoctor.vision.ImagePreprocessor
import com.cropdoctor.vision.ExecuTorchClassifier

enum class AppScreen {
    FIRST_LAUNCH,
    WELCOME,
    HOME,
    CROP_SELECTION,
    CROP_GUIDE_SELECT,
    CAMERA,
    RESULT,
    SAVE_CASE_REPORT,
    DISEASE_DETAILS,
    KNOWLEDGE_BASE,
    LANGUAGE_PICK,
    MORE_OPTIONS,
    FARMING_TIPS,
    CHATBOT,
    SCAN_HISTORY,
    INSTALL_MODEL
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

                    var selectedCrop by remember { mutableStateOf<String?>(null) }
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
                    var currentAdvisorSource by remember { mutableStateOf((application as CropDoctorApplication).advisor.advisorSource) }

                    val app = application as CropDoctorApplication

                    fun navigateTo(screen: AppScreen) {
                        if (screen == AppScreen.CAMERA) {
                            currentRequestId.incrementAndGet()
                            currentDiagnosisJob?.cancel()
                            currentAdviceJob?.cancel()
                            (app.advisor as? ExecuTorchLlamaAdvisor)?.stop()
                            isProcessingImage = false
                            isGeneratingAdvice = false
                        } else if (currentScreen == AppScreen.CAMERA && screen != AppScreen.RESULT) {
                            currentRequestId.incrementAndGet()
                            currentDiagnosisJob?.cancel()
                            currentAdviceJob?.cancel()
                            (app.advisor as? ExecuTorchLlamaAdvisor)?.stop()
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
                                onStateChange = { isGeneratingAdvice = it },
                                onAdvisorSourceChange = { currentAdvisorSource = it }
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
                                onStateChange = { isGeneratingAdvice = it },
                                onAdvisorSourceChange = { currentAdvisorSource = it }
                            )
                        }
                    }

                    // Handle system back button smoothly
                    BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.FIRST_LAUNCH) {
                        when (currentScreen) {
                            AppScreen.RESULT -> navigateTo(AppScreen.HOME)
                            AppScreen.CAMERA -> navigateTo(if (selectedCrop != null) AppScreen.CROP_SELECTION else AppScreen.HOME)
                            AppScreen.CROP_SELECTION -> navigateTo(AppScreen.HOME)
                            AppScreen.CROP_GUIDE_SELECT -> navigateTo(AppScreen.HOME)
                            AppScreen.KNOWLEDGE_BASE -> navigateTo(AppScreen.CROP_GUIDE_SELECT)
                            AppScreen.DISEASE_DETAILS -> navigateTo(AppScreen.KNOWLEDGE_BASE)
                            AppScreen.SAVE_CASE_REPORT -> navigateTo(AppScreen.RESULT)
                            AppScreen.INSTALL_MODEL -> navigateTo(previousScreen)
                            else -> navigateTo(previousScreen)
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
                        Log.i("CROPDOC_LLAMA", "SCAN_START: requestId=$thisRequestId source=$source thread='${Thread.currentThread().name}'")
                        currentDiagnosisJob?.cancel()
                        currentAdviceJob?.cancel()
                        app.advisor.stop("new_scan_processImageBytes_req$thisRequestId")
                        isProcessingImage = true
                        if (pendingCapturedBitmap != null && !pendingCapturedBitmap!!.isRecycled) {
                            pendingCapturedBitmap?.recycle()
                            pendingCapturedBitmap = null
                        }

                        val scanStartTime = System.currentTimeMillis()
                        val watchdogJob = lifecycleScope.launch(Dispatchers.Default) {
                            delay(8000L)
                            if (isProcessingImage) {
                                val elapsed = System.currentTimeMillis() - scanStartTime
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_8S_TRIGGERED: Scan running > 8000ms (${elapsed}ms) for requestId=$thisRequestId isProcessingImage=$isProcessingImage")
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_JOBS: currentDiagnosisJob active=${currentDiagnosisJob?.isActive} cancelled=${currentDiagnosisJob?.isCancelled} completed=${currentDiagnosisJob?.isCompleted}")
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_JOBS: currentAdviceJob active=${currentAdviceJob?.isActive} cancelled=${currentAdviceJob?.isCancelled} completed=${currentAdviceJob?.isCompleted}")
                                val traces = Thread.getAllStackTraces()
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_STACK_TRACES: Total active threads=${traces.size}")
                                for ((thread, stack) in traces) {
                                    val traceStr = stack.take(20).joinToString("\n      at ") { it.toString() }
                                    Log.w("CROPDOC_LLAMA", "THREAD [${thread.name}, id=${thread.id}, state=${thread.state}]:\n      at $traceStr")
                                }
                            }
                        }

                        currentDiagnosisJob = lifecycleScope.launch(Dispatchers.Default) {
                            var preprocessedImage: ImagePreprocessor.PreprocessedImage? = null
                            var bitmapRetained = false
                            try {
                                withTimeout(12_000L) {
                                    val reusableBuf = (app.classifier as? ExecuTorchClassifier)?.getReusableBuffer()
                                    val preprocessed = ImagePreprocessor.prepare(bytes, reusableBuf)
                                    preprocessedImage = preprocessed

                                    if (thisRequestId != currentRequestId.get() || !isActive) {
                                        preprocessed.bitmap512.recycle()
                                        return@withTimeout
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
                                        withContext(NonCancellable + Dispatchers.Main) {
                                            if (thisRequestId != currentRequestId.get()) {
                                                return@withContext
                                            }
                                            if (pendingCapturedBitmap != null && pendingCapturedBitmap != preprocessed.bitmap512 && !pendingCapturedBitmap!!.isRecycled) {
                                                pendingCapturedBitmap?.recycle()
                                            }
                                            pendingCapturedBitmap = preprocessed.bitmap512
                                            bitmapRetained = true
                                            pendingCropFilter = effectiveCropFilter
                                            pendingMetadata = meta
                                            showSoftCropWarningDialog = true
                                            isProcessingImage = false
                                        }
                                        return@withTimeout
                                    }

                                    val result = app.classifier.classify(preprocessed, effectiveCropFilter, meta)

                                    if (thisRequestId != currentRequestId.get() || !isActive) {
                                        preprocessed.bitmap512.recycle()
                                        return@withTimeout
                                    }

                                val kb = KnowledgeBase.getEntry(result.classId)
                                    ?: if (result.outcome == DiagnosisOutcome.OUT_OF_CLASS || result.classId == "out_of_class") {
                                        KbEntry(
                                            classId = "out_of_class",
                                            index = -1,
                                            crop = result.selectedCrop ?: effectiveCropFilter ?: "crop",
                                            type = "out_of_class",
                                            pathogen = "Unsupported / Out of Class",
                                            names = LocalizedText(
                                                en = "Out of class",
                                                te = "గుర్తించిన పంట సమస్య కాదు",
                                                hi = "समर्थित वर्ग से बाहर"
                                            ),
                                            referralThreshold = "MILD",
                                            immediateStep = LocalizedText(
                                                en = "Choose the right crop or ask an extension officer.",
                                                te = "సరైన పంటను ఎంచుకోండి లేదా వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి.",
                                                hi = "सही फसल चुनें या कृषि विस्तार अधिकारी से संपर्क करें।"
                                            ),
                                            explainText = LocalizedText(
                                                en = "This does not match the selected crop or one of the 20 problems we support. Choose the right crop or ask an extension officer.",
                                                te = "ఈ సమస్య మద్దతు ఉన్న జాబితాలో లేదు. ఇది ఎంచుకున్న పంట లేదా మేము గుర్తించే 20 సమస్యలలో దేనికీ సరిపోలడం లేదు. సరైన పంటను ఎంచుకోండి లేదా వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి.",
                                                hi = "समर्थित वर्ग से बाहर। यह चुनी गई फसल या हमारे द्वारा समर्थित 20 समस्याओं में से किसी से मेल नहीं खाता है। सही फसल चुनें या कृषि विस्तार अधिकारी से संपर्क करें।"
                                            ),
                                            treatment = TreatmentPlan()
                                        )
                                    } else if (result.classId != "unknown") {
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

                                withContext(NonCancellable + Dispatchers.Main) {
                                    if (thisRequestId != currentRequestId.get()) {
                                        return@withContext
                                    }
                                    if (capturedLeafBitmap != null && capturedLeafBitmap != preprocessed.bitmap512 && !capturedLeafBitmap!!.isRecycled) {
                                        capturedLeafBitmap?.recycle()
                                    }
                                    capturedLeafBitmap = preprocessed.bitmap512
                                    bitmapRetained = true
                                    classifierResult = result
                                    activeKbEntry = kb
                                    isProcessingImage = false
                                    navigateTo(AppScreen.RESULT)

                                    if (autoStartAdvisor && result.outcome == DiagnosisOutcome.DIAGNOSE) {
                                        triggerAdvisoryStream(
                                            app = app,
                                            result = result,
                                            kbEntry = kb,
                                            language = selectedLanguage,
                                            crop = result.selectedCrop ?: effectiveCropFilter,
                                            requestId = thisRequestId,
                                            onUpdate = { advisoryText = it },
                                            onStateChange = { isGeneratingAdvice = it },
                                            onAdvisorSourceChange = { currentAdvisorSource = it }
                                        )
                                    }
                                }
                            }
                            } catch (e: TimeoutCancellationException) {
                                Log.e("MainActivity", "Diagnosis timed out after 10s", e)
                                val effectiveCropFilter = cropFilter ?: selectedCrop
                                val timeoutResult = ClassifierResult(
                                    classId = "unknown",
                                    displayName = "Could not read the photo. Please try again.",
                                    confidence = 0f,
                                    severityBand = SeverityBand.MILD,
                                    certaintyBand = CertaintyBand.NOT_SURE,
                                    lesionFraction = 0f,
                                    outcome = DiagnosisOutcome.CANT_TELL,
                                    outcomeReason = "TIMEOUT: Analysis exceeded 10s limit",
                                    selectedCrop = effectiveCropFilter
                                )
                                withContext(NonCancellable + Dispatchers.Main) {
                                    if (thisRequestId == currentRequestId.get()) {
                                        classifierResult = timeoutResult
                                        activeKbEntry = KnowledgeBase.getEntry("unknown") ?: KnowledgeBase.getAllEntries().firstOrNull()
                                        isProcessingImage = false
                                        navigateTo(AppScreen.RESULT)
                                    }
                                }
                            } catch (e: Exception) {
                                if (e !is CancellationException) {
                                    Log.e("MainActivity", "Diagnosis error", e)
                                    val effectiveCropFilter = cropFilter ?: selectedCrop
                                    val errorResult = ClassifierResult(
                                        classId = "unknown",
                                        displayName = "Could not read the photo. Please try again.",
                                        confidence = 0f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0f,
                                        outcome = DiagnosisOutcome.CANT_TELL,
                                        outcomeReason = "ERROR: ${e.message ?: "Could not read photo"}",
                                        selectedCrop = effectiveCropFilter
                                    )
                                    withContext(NonCancellable + Dispatchers.Main) {
                                        if (thisRequestId == currentRequestId.get()) {
                                            classifierResult = errorResult
                                            activeKbEntry = KnowledgeBase.getEntry("unknown") ?: KnowledgeBase.getAllEntries().firstOrNull()
                                            isProcessingImage = false
                                            navigateTo(AppScreen.RESULT)
                                        }
                                    }
                                }
                            } finally {
                                watchdogJob.cancel()
                                if (!bitmapRetained) {
                                    preprocessedImage?.bitmap512?.recycle()
                                }
                                withContext(NonCancellable + Dispatchers.Main) {
                                    if (thisRequestId == currentRequestId.get()) {
                                        isProcessingImage = false
                                    }
                                }
                                val scanDuration = System.currentTimeMillis() - scanStartTime
                                Log.i("CROPDOC_LLAMA", "SCAN_END: requestId=$thisRequestId durationMs=${scanDuration}ms thread='${Thread.currentThread().name}'")
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
                        Log.i("CROPDOC_LLAMA", "SCAN_START: requestId=$thisRequestId source=bitmap thread='${Thread.currentThread().name}'")
                        currentDiagnosisJob?.cancel()
                        currentAdviceJob?.cancel()
                        app.advisor.stop("new_scan_processDiagnosis_req$thisRequestId")
                        isProcessingImage = true

                        val scanStartTime = System.currentTimeMillis()
                        val watchdogJob = lifecycleScope.launch(Dispatchers.Default) {
                            delay(8000L)
                            if (isProcessingImage) {
                                val elapsed = System.currentTimeMillis() - scanStartTime
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_8S_TRIGGERED: Scan running > 8000ms (${elapsed}ms) for requestId=$thisRequestId isProcessingImage=$isProcessingImage")
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_JOBS: currentDiagnosisJob active=${currentDiagnosisJob?.isActive} cancelled=${currentDiagnosisJob?.isCancelled} completed=${currentDiagnosisJob?.isCompleted}")
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_JOBS: currentAdviceJob active=${currentAdviceJob?.isActive} cancelled=${currentAdviceJob?.isCancelled} completed=${currentAdviceJob?.isCompleted}")
                                val traces = Thread.getAllStackTraces()
                                Log.w("CROPDOC_LLAMA", "WATCHDOG_STACK_TRACES: Total active threads=${traces.size}")
                                for ((thread, stack) in traces) {
                                    val traceStr = stack.take(20).joinToString("\n      at ") { it.toString() }
                                    Log.w("CROPDOC_LLAMA", "THREAD [${thread.name}, id=${thread.id}, state=${thread.state}]:\n      at $traceStr")
                                }
                            }
                        }

                        currentDiagnosisJob = lifecycleScope.launch(Dispatchers.Default) {
                            var bitmapRetained = false
                            try {
                                withTimeout(12_000L) {
                                    val effectiveCropFilter = cropFilter ?: selectedCrop
                                    val result = app.classifier.classify(bitmap, effectiveCropFilter, metadata)

                                    if (thisRequestId != currentRequestId.get() || !isActive) {
                                        return@withTimeout
                                    }

                                    val kb = KnowledgeBase.getEntry(result.classId)
                                        ?: if (result.outcome == DiagnosisOutcome.OUT_OF_CLASS || result.classId == "out_of_class") {
                                            KbEntry(
                                                classId = "out_of_class",
                                                index = -1,
                                                crop = result.selectedCrop ?: effectiveCropFilter ?: "crop",
                                                type = "out_of_class",
                                                pathogen = "Unsupported / Out of Class",
                                                names = LocalizedText(
                                                    en = "Out of class",
                                                    te = "గుర్తించిన పంట సమస్య కాదు",
                                                    hi = "समर्थित वर्ग से बाहर"
                                                ),
                                                referralThreshold = "MILD",
                                                immediateStep = LocalizedText(
                                                    en = "Choose the right crop or ask an extension officer.",
                                                    te = "సరైన పంటను ఎంచుకోండి లేదా వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి.",
                                                    hi = "सही फसल चुनें या कृषि विस्तार अधिकारी से संपर्क करें।"
                                                ),
                                                explainText = LocalizedText(
                                                    en = "This does not match the selected crop or one of the 20 problems we support. Choose the right crop or ask an extension officer.",
                                                    te = "ఈ సమస్య మద్దతు ఉన్న జాబితాలో లేదు. ఇది ఎంచుకున్న పంట లేదా మేము గుర్తించే 20 సమస్యలలో దేనికీ సరిపోలడం లేదు. సరైన పంటను ఎంచుకోండి లేదా వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి.",
                                                    hi = "समर्थित वर्ग से बाहर। यह चुनी गई फसल या हमारे द्वारा समर्थित 20 समस्याओं में से किसी से मेल नहीं खाता है। सही फसल चुनें या कृषि विस्तार अधिकारी से संपर्क करें।"
                                                ),
                                                treatment = TreatmentPlan()
                                            )
                                        } else if (result.classId != "unknown") {
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

                                    withContext(NonCancellable + Dispatchers.Main) {
                                        if (thisRequestId != currentRequestId.get()) return@withContext
                                        if (capturedLeafBitmap != null && capturedLeafBitmap != bitmap && !capturedLeafBitmap!!.isRecycled) {
                                            capturedLeafBitmap?.recycle()
                                        }
                                        capturedLeafBitmap = bitmap
                                        bitmapRetained = true
                                        classifierResult = result
                                        activeKbEntry = kb
                                        isProcessingImage = false
                                        navigateTo(AppScreen.RESULT)

                                        if (autoStartAdvisor && result.outcome == DiagnosisOutcome.DIAGNOSE) {
                                            triggerAdvisoryStream(
                                                app = app,
                                                result = result,
                                                kbEntry = kb,
                                                language = selectedLanguage,
                                                crop = result.selectedCrop ?: effectiveCropFilter,
                                                requestId = thisRequestId,
                                                onUpdate = { advisoryText = it },
                                                onStateChange = { isGeneratingAdvice = it },
                                                onAdvisorSourceChange = { currentAdvisorSource = it }
                                            )
                                        }
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                Log.e("MainActivity", "Diagnosis timed out after 10s", e)
                                val effectiveCropFilter = cropFilter ?: selectedCrop
                                val timeoutResult = ClassifierResult(
                                    classId = "unknown",
                                    displayName = "Could not read the photo. Please try again.",
                                    confidence = 0f,
                                    severityBand = SeverityBand.MILD,
                                    certaintyBand = CertaintyBand.NOT_SURE,
                                    lesionFraction = 0f,
                                    outcome = DiagnosisOutcome.CANT_TELL,
                                    outcomeReason = "TIMEOUT: Analysis exceeded 10s limit",
                                    selectedCrop = effectiveCropFilter
                                )
                                withContext(NonCancellable + Dispatchers.Main) {
                                    if (thisRequestId == currentRequestId.get()) {
                                        classifierResult = timeoutResult
                                        activeKbEntry = KnowledgeBase.getEntry("unknown") ?: KnowledgeBase.getAllEntries().firstOrNull()
                                        isProcessingImage = false
                                        navigateTo(AppScreen.RESULT)
                                    }
                                }
                            } catch (e: Exception) {
                                if (e !is CancellationException) {
                                    Log.e("MainActivity", "Diagnosis error", e)
                                    val effectiveCropFilter = cropFilter ?: selectedCrop
                                    val errorResult = ClassifierResult(
                                        classId = "unknown",
                                        displayName = "Could not read the photo. Please try again.",
                                        confidence = 0f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0f,
                                        outcome = DiagnosisOutcome.CANT_TELL,
                                        outcomeReason = "ERROR: ${e.message ?: "Could not read photo"}",
                                        selectedCrop = effectiveCropFilter
                                    )
                                    withContext(NonCancellable + Dispatchers.Main) {
                                        if (thisRequestId == currentRequestId.get()) {
                                            classifierResult = errorResult
                                            activeKbEntry = KnowledgeBase.getEntry("unknown") ?: KnowledgeBase.getAllEntries().firstOrNull()
                                            isProcessingImage = false
                                            navigateTo(AppScreen.RESULT)
                                        }
                                    }
                                }
                            } finally {
                                watchdogJob.cancel()
                                if (!bitmapRetained && capturedLeafBitmap != bitmap && !bitmap.isRecycled) {
                                    bitmap.recycle()
                                }
                                withContext(NonCancellable + Dispatchers.Main) {
                                    if (thisRequestId == currentRequestId.get()) {
                                        isProcessingImage = false
                                    }
                                }
                                val scanDuration = System.currentTimeMillis() - scanStartTime
                                Log.i("CROPDOC_LLAMA", "SCAN_END: requestId=$thisRequestId durationMs=${scanDuration}ms thread='${Thread.currentThread().name}'")
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
                                val langExtra = intent.getStringExtra("language")
                                if (langExtra != null && (langExtra == "en" || langExtra == "hi" || langExtra == "te")) {
                                    selectedLanguage = langExtra
                                    userPrefs.selectedLanguage = langExtra
                                }
                                if (filePath != null) {
                                    try {
                                        val file = File(filePath)
                                        if (file.exists()) {
                                            val bytes = file.readBytes()
                                            val autoStart = intent.getBooleanExtra("auto_start_advisor", true)
                                            processImageBytes(bytes, source, cropHint, autoStartAdvisor = autoStart, skipSoftWarning = true)
                                        } else {
                                            Log.e("MainActivityBenchmark", "diagnose_file: file not found: $filePath")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivityBenchmark", "diagnose_file error", e)
                                    }
                                }
                            }

                            if (action == "test_chat") {
                                val query = intent.getStringExtra("chat_query") ?: "What is the natural remedy?"
                                val lang = intent.getStringExtra("language") ?: "en"
                                val classId = intent.getStringExtra("class_id") ?: (classifierResult?.classId ?: "tomato_yellow_leaf_curl_virus")
                                val crop = intent.getStringExtra("crop") ?: (activeKbEntry?.crop ?: "tomato")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val store = app.getPassageStoreIfInitialized()
                                    val adv = app.getAdvisorIfInitialized()
                                    if (store != null && adv != null) {
                                        Log.i("CROPDOC_CHAT", "CHAT_TEST_START: query='$query' lang='$lang' classId='$classId'")
                                        val startTime = System.currentTimeMillis()
                                        val response = ChatEngine.processQuery(
                                            query = query,
                                            detectedClass = classId,
                                            crop = crop,
                                            problemName = activeKbEntry?.names?.forLanguage(lang) ?: classId,
                                            severity = classifierResult?.severityBand?.name ?: "MODERATE",
                                            certainty = classifierResult?.certaintyBand?.name ?: "SURE",
                                            language = lang,
                                            passageStore = store,
                                            isUncertainOrOutOfClass = false,
                                            llamaGenerator = if (adv.isModelAvailable) {
                                                { prompt -> adv.answerChatPrompt(prompt) }
                                            } else null
                                        )
                                        val durationMs = System.currentTimeMillis() - startTime
                                        val pss = Debug.getPss() / 1024L
                                        Log.i("CROPDOC_CHAT", "CHAT_TEST_END: query='$query' lang='$lang' source='${response.source}' label='${response.label}' durationMs=${durationMs}ms pssMb=${pss}MB textLength=${response.text.length} isNoMatch=${response.isNoMatch} response='${response.text.replace("\n", " ")}'")
                                    }
                                }
                            }

                            if (action == "test_voice") {
                                val lang = intent.getStringExtra("language") ?: "en"
                                val filePath = intent.getStringExtra("file_path")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val recognizer = VoskVoiceRecognizer(this@MainActivity)
                                    Log.i("CROPDOC_VOICE", "VOICE_TEST_START: lang='$lang' file='$filePath'")
                                    val startTime = System.currentTimeMillis()
                                    try {
                                        val wavFile = if (filePath != null) File(filePath) else null
                                        val result = if (wavFile != null && wavFile.exists()) {
                                            recognizer.recognizeWavFile(wavFile, lang)
                                        } else {
                                            val dummyWav = File(cacheDir, "test_tone.wav")
                                            if (!dummyWav.exists()) {
                                                dummyWav.writeBytes(ByteArray(32000))
                                            }
                                            recognizer.recognizeWavFile(dummyWav, lang)
                                        }
                                        val durationMs = System.currentTimeMillis() - startTime
                                        val pss = Debug.getPss() / 1024L
                                        Log.i("CROPDOC_VOICE", "VOICE_TEST_END: lang='$lang' text='${result.text}' durationMs=${durationMs}ms pssMb=${pss}MB")
                                    } catch (e: Exception) {
                                        Log.e("CROPDOC_VOICE", "VOICE_TEST_ERROR: lang='$lang' error=${e.message}", e)
                                    } finally {
                                        recognizer.releaseModel()
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
                                        onStateChange = { isGeneratingAdvice = it },
                                        onAdvisorSourceChange = { currentAdvisorSource = it }
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
                                "show_home_en" -> {
                                    switchLanguage("en")
                                    navigateTo(AppScreen.HOME)
                                }
                                "show_home_te" -> {
                                    switchLanguage("te")
                                    navigateTo(AppScreen.HOME)
                                }
                                "show_home_hi" -> {
                                    switchLanguage("hi")
                                    navigateTo(AppScreen.HOME)
                                }
                                "show_camera_en" -> {
                                    switchLanguage("en")
                                    navigateTo(AppScreen.CAMERA)
                                }
                                "show_camera_te" -> {
                                    switchLanguage("te")
                                    navigateTo(AppScreen.CAMERA)
                                }
                                "show_camera_hi" -> {
                                    switchLanguage("hi")
                                    navigateTo(AppScreen.CAMERA)
                                }
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
                                "show_outcome_diagnose", "show_outcome_diagnose_en" -> {
                                    switchLanguage("en")
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("chilli_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "chilli_leaf_curl_virus",
                                        displayName = kb.names.forLanguage("en"),
                                        confidence = 0.92f,
                                        severityBand = SeverityBand.MODERATE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.20f,
                                        inferenceTimeMs = 84L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = "chilli",
                                        cropMass = 0.98f,
                                        outcome = DiagnosisOutcome.DIAGNOSE,
                                        outcomeReason = "ACCEPTED",
                                        margin = 0.85f,
                                        freeEnergy = -16.5
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_diagnose_te" -> {
                                    switchLanguage("te")
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("chilli_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "chilli_leaf_curl_virus",
                                        displayName = kb.names.forLanguage("te"),
                                        confidence = 0.92f,
                                        severityBand = SeverityBand.MODERATE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.20f,
                                        inferenceTimeMs = 84L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = "chilli",
                                        cropMass = 0.98f,
                                        outcome = DiagnosisOutcome.DIAGNOSE,
                                        outcomeReason = "ACCEPTED",
                                        margin = 0.85f,
                                        freeEnergy = -16.5
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_diagnose_hi" -> {
                                    switchLanguage("hi")
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("chilli_leaf_curl_virus")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    val result = ClassifierResult(
                                        classId = "chilli_leaf_curl_virus",
                                        displayName = kb.names.forLanguage("hi"),
                                        confidence = 0.92f,
                                        severityBand = SeverityBand.MODERATE,
                                        certaintyBand = CertaintyBand.SURE,
                                        lesionFraction = 0.20f,
                                        inferenceTimeMs = 84L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = "chilli",
                                        cropMass = 0.98f,
                                        outcome = DiagnosisOutcome.DIAGNOSE,
                                        outcomeReason = "ACCEPTED",
                                        margin = 0.85f,
                                        freeEnergy = -16.5
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_cant_tell", "show_outcome_cant_tell_en" -> {
                                    switchLanguage("en")
                                    val kb = KbEntry(
                                        classId = "unknown",
                                        index = -1,
                                        crop = "crop",
                                        type = "unknown",
                                        pathogen = "Unknown",
                                        names = LocalizedText("Not sure", "ఖచ్చితంగా తెలియదు", "अनिश्चित स्थिति"),
                                        referralThreshold = "MILD",
                                        immediateStep = LocalizedText("", "", ""),
                                        explainText = LocalizedText("", "", ""),
                                        treatment = TreatmentPlan()
                                    )
                                    val result = ClassifierResult(
                                        classId = "unknown",
                                        displayName = "Not sure - ask an extension officer",
                                        confidence = 0.22f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0.0f,
                                        inferenceTimeMs = 65L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = selectedCrop,
                                        cropMass = 0.15f,
                                        outcome = DiagnosisOutcome.CANT_TELL,
                                        outcomeReason = "HIGH_FREE_ENERGY(-5.4 > -8.0)",
                                        margin = 0.05f,
                                        freeEnergy = -5.4
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_cant_tell_te" -> {
                                    switchLanguage("te")
                                    val kb = KbEntry(
                                        classId = "unknown",
                                        index = -1,
                                        crop = "crop",
                                        type = "unknown",
                                        pathogen = "Unknown",
                                        names = LocalizedText("Not sure", "ఖచ్చితంగా తెలియదు", "अनिश्चित स्थिति"),
                                        referralThreshold = "MILD",
                                        immediateStep = LocalizedText("", "", ""),
                                        explainText = LocalizedText("", "", ""),
                                        treatment = TreatmentPlan()
                                    )
                                    val result = ClassifierResult(
                                        classId = "unknown",
                                        displayName = "ఖచ్చితంగా తెలియదు",
                                        confidence = 0.22f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0.0f,
                                        inferenceTimeMs = 65L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = selectedCrop,
                                        cropMass = 0.15f,
                                        outcome = DiagnosisOutcome.CANT_TELL,
                                        outcomeReason = "HIGH_FREE_ENERGY(-5.4 > -8.0)",
                                        margin = 0.05f,
                                        freeEnergy = -5.4
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_cant_tell_hi" -> {
                                    switchLanguage("hi")
                                    val kb = KbEntry(
                                        classId = "unknown",
                                        index = -1,
                                        crop = "crop",
                                        type = "unknown",
                                        pathogen = "Unknown",
                                        names = LocalizedText("Not sure", "ఖచ్చితంగా తెలియదు", "अनिश्चित स्थिति"),
                                        referralThreshold = "MILD",
                                        immediateStep = LocalizedText("", "", ""),
                                        explainText = LocalizedText("", "", ""),
                                        treatment = TreatmentPlan()
                                    )
                                    val result = ClassifierResult(
                                        classId = "unknown",
                                        displayName = "अनिश्चित स्थिति",
                                        confidence = 0.22f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0.0f,
                                        inferenceTimeMs = 65L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = selectedCrop,
                                        cropMass = 0.15f,
                                        outcome = DiagnosisOutcome.CANT_TELL,
                                        outcomeReason = "HIGH_FREE_ENERGY(-5.4 > -8.0)",
                                        margin = 0.05f,
                                        freeEnergy = -5.4
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_out_of_class", "show_outcome_out_of_class_en" -> {
                                    switchLanguage("en")
                                    val kb = KbEntry(
                                        classId = "out_of_class",
                                        index = -1,
                                        crop = selectedCrop ?: "crop",
                                        type = "out_of_class",
                                        pathogen = "Unsupported / Out of Class",
                                        names = LocalizedText("Out of class", "గుర్తించిన పంట సమస్య కాదు", "समर्थित वर्ग से बाहर"),
                                        referralThreshold = "MILD",
                                        immediateStep = LocalizedText("", "", ""),
                                        explainText = LocalizedText("", "", ""),
                                        treatment = TreatmentPlan()
                                    )
                                    val result = ClassifierResult(
                                        classId = "out_of_class",
                                        displayName = "Out of class - plant does not match",
                                        confidence = 0.42f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0.0f,
                                        inferenceTimeMs = 70L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = selectedCrop,
                                        cropMass = 0.18f,
                                        outcome = DiagnosisOutcome.OUT_OF_CLASS,
                                        outcomeReason = "LOW_CROP_MASS(0.18 < 0.60)",
                                        margin = 0.10f,
                                        freeEnergy = -12.3
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_out_of_class_te" -> {
                                    switchLanguage("te")
                                    val kb = KbEntry(
                                        classId = "out_of_class",
                                        index = -1,
                                        crop = selectedCrop ?: "crop",
                                        type = "out_of_class",
                                        pathogen = "Unsupported / Out of Class",
                                        names = LocalizedText("Out of class", "గుర్తించిన పంట సమస్య కాదు", "समर्थित वर्ग से बाहर"),
                                        referralThreshold = "MILD",
                                        immediateStep = LocalizedText("", "", ""),
                                        explainText = LocalizedText("", "", ""),
                                        treatment = TreatmentPlan()
                                    )
                                    val result = ClassifierResult(
                                        classId = "out_of_class",
                                        displayName = "గుర్తించిన పంట సమస్య కాదు",
                                        confidence = 0.42f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0.0f,
                                        inferenceTimeMs = 70L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = selectedCrop,
                                        cropMass = 0.18f,
                                        outcome = DiagnosisOutcome.OUT_OF_CLASS,
                                        outcomeReason = "LOW_CROP_MASS(0.18 < 0.60)",
                                        margin = 0.10f,
                                        freeEnergy = -12.3
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
                                }
                                "show_outcome_out_of_class_hi" -> {
                                    switchLanguage("hi")
                                    val kb = KbEntry(
                                        classId = "out_of_class",
                                        index = -1,
                                        crop = selectedCrop ?: "crop",
                                        type = "out_of_class",
                                        pathogen = "Unsupported / Out of Class",
                                        names = LocalizedText("Out of class", "గుర్తించిన పంట సమస్య కాదు", "समर्थित वर्ग से बाहर"),
                                        referralThreshold = "MILD",
                                        immediateStep = LocalizedText("", "", ""),
                                        explainText = LocalizedText("", "", ""),
                                        treatment = TreatmentPlan()
                                    )
                                    val result = ClassifierResult(
                                        classId = "out_of_class",
                                        displayName = "समर्थित वर्ग से बाहर",
                                        confidence = 0.42f,
                                        severityBand = SeverityBand.MILD,
                                        certaintyBand = CertaintyBand.NOT_SURE,
                                        lesionFraction = 0.0f,
                                        inferenceTimeMs = 70L,
                                        memoryUsedMb = 120L,
                                        isDemoMode = false,
                                        selectedCrop = selectedCrop,
                                        cropMass = 0.18f,
                                        outcome = DiagnosisOutcome.OUT_OF_CLASS,
                                        outcomeReason = "LOW_CROP_MASS(0.18 < 0.60)",
                                        margin = 0.10f,
                                        freeEnergy = -12.3
                                    )
                                    classifierResult = result
                                    activeKbEntry = kb
                                    navigateTo(AppScreen.RESULT)
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
                                "show_home" -> navigateTo(AppScreen.HOME)
                                "show_camera" -> navigateTo(AppScreen.CAMERA)
                                "show_crop_guide_select_en" -> {
                                    switchLanguage("en")
                                    navigateTo(AppScreen.CROP_GUIDE_SELECT)
                                }
                                "show_crop_guide_select_te" -> {
                                    switchLanguage("te")
                                    navigateTo(AppScreen.CROP_GUIDE_SELECT)
                                }
                                "show_crop_guide_select_hi" -> {
                                    switchLanguage("hi")
                                    navigateTo(AppScreen.CROP_GUIDE_SELECT)
                                }
                                "show_crop_guide_problems_en" -> {
                                    selectedCrop = "rice"
                                    switchLanguage("en")
                                    navigateTo(AppScreen.KNOWLEDGE_BASE)
                                }
                                "show_crop_guide_problems_te" -> {
                                    selectedCrop = "rice"
                                    switchLanguage("te")
                                    navigateTo(AppScreen.KNOWLEDGE_BASE)
                                }
                                "show_crop_guide_problems_hi" -> {
                                    selectedCrop = "rice"
                                    switchLanguage("hi")
                                    navigateTo(AppScreen.KNOWLEDGE_BASE)
                                }
                                "show_crop_guide_details_en" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("rice_bacterial_leaf_blight")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    selectedDetailEntry = kb
                                    switchLanguage("en")
                                    navigateTo(AppScreen.DISEASE_DETAILS)
                                }
                                "show_crop_guide_details_te" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("rice_bacterial_leaf_blight")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    selectedDetailEntry = kb
                                    switchLanguage("te")
                                    navigateTo(AppScreen.DISEASE_DETAILS)
                                }
                                "show_crop_guide_details_hi" -> {
                                    KnowledgeBase.initialize(this@MainActivity)
                                    val kb = KnowledgeBase.getEntry("rice_bacterial_leaf_blight")
                                        ?: KnowledgeBase.getAllEntries().first()
                                    selectedDetailEntry = kb
                                    switchLanguage("hi")
                                    navigateTo(AppScreen.DISEASE_DETAILS)
                                }
                            }
                        }
                        intent?.let { inIntent ->
                            inIntent.getStringExtra("benchmark_action")?.let { action ->
                                onBenchmarkAction?.invoke(action)
                            }
                            onBenchmarkIntent?.invoke(inIntent)
                        }
                        onDispose {
                            onBenchmarkAction = null
                            onBenchmarkIntent = null
                        }
                    }

                    // Soft warning dialog for non-standard crop photos (never blocks, offers retake or proceed)
                    if (showSoftCropWarningDialog && pendingCapturedBitmap != null) {
                        AlertDialog(
                            onDismissRequest = {
                                showSoftCropWarningDialog = false
                                pendingCapturedBitmap?.recycle()
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
                                    pendingCapturedBitmap?.recycle()
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
                                onScanClick = {
                                    selectedCrop = null
                                    navigateTo(AppScreen.CAMERA)
                                },
                                onBrowseDiseasesClick = { navigateTo(AppScreen.CROP_GUIDE_SELECT) },
                                onFarmingTipsClick = { navigateTo(AppScreen.FARMING_TIPS) },
                                onChangeLanguageClick = { newLang -> switchLanguage(newLang) },
                                onMoreOptionsClick = { navigateTo(AppScreen.MORE_OPTIONS) },
                                onChatbotClick = { navigateTo(AppScreen.CHATBOT) },
                                onSavedReportsClick = { navigateTo(AppScreen.SCAN_HISTORY) },
                                onHelpOfficerClick = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001801551"))
                                        startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Failed to launch dialer", e)
                                    }
                                }
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

                        AppScreen.CROP_GUIDE_SELECT -> {
                            CropSelectionScreen(
                                language = selectedLanguage,
                                customTitle = stringResource(R.string.crop_guide_title),
                                customSubtitle = stringResource(R.string.select_crop_guide_subtitle),
                                onCropSelected = { crop ->
                                    selectedCrop = crop
                                    navigateTo(AppScreen.KNOWLEDGE_BASE)
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
                                onShutterClick = {
                                    currentAdviceJob?.cancel()
                                    (app.advisor as? ExecuTorchLlamaAdvisor)?.stop()
                                    isGeneratingAdvice = false
                                },
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
                                onBackClick = { navigateTo(if (selectedCrop != null) AppScreen.CROP_SELECTION else AppScreen.HOME) },
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
                                    advisorSource = currentAdvisorSource,
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
                                                onStateChange = { isGeneratingAdvice = it },
                                                onAdvisorSourceChange = { currentAdvisorSource = it }
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
                                    },
                                    onChangeCropClick = { navigateTo(AppScreen.CROP_SELECTION) },
                                    onChangeCropSelected = { newCrop ->
                                        selectedCrop = newCrop
                                        val bmp = capturedLeafBitmap
                                        if (bmp != null && !bmp.isRecycled) {
                                            processDiagnosis(bmp, cropFilter = newCrop)
                                        }
                                    },
                                    onTakeAnotherPhoto = { navigateTo(AppScreen.CAMERA) },
                                    onInstallModelClick = { navigateTo(AppScreen.INSTALL_MODEL) },
                                    advisor = app.getAdvisorIfInitialized(),
                                    passageStore = app.getPassageStoreIfInitialized()
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
                                    advisorSource = currentAdvisorSource,
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
                                onBackClick = { navigateTo(AppScreen.KNOWLEDGE_BASE) },
                                onBackToHome = { navigateTo(AppScreen.HOME) }
                            )
                        }

                        AppScreen.KNOWLEDGE_BASE -> {
                            KnowledgeBaseScreen(
                                crop = selectedCrop ?: "rice",
                                language = selectedLanguage,
                                onBackClick = { navigateTo(AppScreen.CROP_GUIDE_SELECT) },
                                onChangeCropClick = { navigateTo(AppScreen.CROP_GUIDE_SELECT) },
                                onProblemClick = { entry ->
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
                                onScanHistoryClick = { navigateTo(AppScreen.SCAN_HISTORY) },
                                onInstallModelClick = { navigateTo(AppScreen.INSTALL_MODEL) }
                            )
                        }

                        AppScreen.INSTALL_MODEL -> {
                            ModelInstallScreen(
                                language = selectedLanguage,
                                onBackClick = { navigateTo(previousScreen) }
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
        requestId: Long = 0L,
        onUpdate: (String) -> Unit,
        onStateChange: (Boolean) -> Unit,
        onAdvisorSourceChange: ((String) -> Unit)? = null
    ) {
        currentAdviceJob?.cancel()
        app.advisor.stop("triggerAdvisoryStream_req$requestId")
        onUpdate("")
        onStateChange(true)
        onAdvisorSourceChange?.invoke(app.advisor.advisorSource)

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
                val ready = app.advisor.cancelAndAwait(3000L)
                if (!ready) {
                    Log.w("MainActivity", "Previous advisor did not exit in 3s; using verified offline guide")
                }
                app.advisor.generateAdvice(
                    classId = result.classId,
                    severityBand = result.severityBand,
                    certainty = result.certaintyBand.name,
                    language = language,
                    kbEntry = kbEntry,
                    fieldSpread = spreadStr,
                    region = region,
                    urgency = urgency,
                    crop = effectiveCrop,
                    requestId = requestId
                ) { token ->
                    sb.append(token)
                    onUpdate(sb.toString())
                    onAdvisorSourceChange?.invoke(app.advisor.advisorSource)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onAdvisorSourceChange?.invoke(app.advisor.advisorSource)
                onStateChange(false)
            }
        }
    }
}
