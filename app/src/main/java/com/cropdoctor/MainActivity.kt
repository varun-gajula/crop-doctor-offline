package com.cropdoctor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.ui.LanguagePickScreen
import com.cropdoctor.ui.MainCameraScreen
import com.cropdoctor.ui.ResultScreen
import com.cropdoctor.ui.theme.CropDoctorTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var currentAdviceJob: Job? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission state handled gracefully in UI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Camera permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CropDoctorTheme {
                var selectedLanguage by remember { mutableStateOf<String?>("te") } // Default to Telugu on launch or allow picking
                var showLanguagePicker by remember { mutableStateOf(false) }

                var classifierResult by remember { mutableStateOf<ClassifierResult?>(null) }
                var activeKbEntry by remember { mutableStateOf<KbEntry?>(null) }
                var isProcessingImage by remember { mutableStateOf(false) }

                var advisoryText by remember { mutableStateOf("") }
                var isGeneratingAdvice by remember { mutableStateOf(false) }

                val app = application as CropDoctorApplication

                if (selectedLanguage == null || showLanguagePicker) {
                    LanguagePickScreen(
                        onLanguageSelected = { lang ->
                            selectedLanguage = lang
                            showLanguagePicker = false
                            // If currently on result screen, trigger re-stream of advice in the new language
                            val currentRes = classifierResult
                            val currentKb = activeKbEntry
                            if (currentRes != null && currentKb != null) {
                                triggerAdvisoryStream(
                                    app = app,
                                    result = currentRes,
                                    kbEntry = currentKb,
                                    language = lang,
                                    onUpdate = { advisoryText = it },
                                    onStateChange = { isGeneratingAdvice = it }
                                )
                            }
                        }
                    )
                } else if (classifierResult != null && activeKbEntry != null) {
                    ResultScreen(
                        result = classifierResult!!,
                        kbEntry = activeKbEntry!!,
                        language = selectedLanguage!!,
                        advisoryText = advisoryText,
                        isGeneratingAdvice = isGeneratingAdvice,
                        onRetakeClick = {
                            currentAdviceJob?.cancel()
                            classifierResult = null
                            activeKbEntry = null
                            advisoryText = ""
                            isGeneratingAdvice = false
                        },
                        onChangeLanguageClick = {
                            showLanguagePicker = true
                        }
                    )
                } else {
                    MainCameraScreen(
                        language = selectedLanguage!!,
                        isProcessing = isProcessingImage,
                        onImageCaptured = { bitmap ->
                            isProcessingImage = true
                            lifecycleScope.launch {
                                try {
                                    val result = app.classifier.classify(bitmap)
                                    val kb = KnowledgeBase.getEntry(result.classId)
                                        ?: KnowledgeBase.getAllEntries().firstOrNull()

                                    if (kb != null) {
                                        classifierResult = result
                                        activeKbEntry = kb

                                        // Instantly display result screen, stream Llama advisory asynchronously
                                        triggerAdvisoryStream(
                                            app = app,
                                            result = result,
                                            kbEntry = kb,
                                            language = selectedLanguage!!,
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
                        onChangeLanguageClick = {
                            showLanguagePicker = true
                        }
                    )
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
