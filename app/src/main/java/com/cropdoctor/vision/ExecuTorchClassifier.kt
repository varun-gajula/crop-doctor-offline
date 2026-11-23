package com.cropdoctor.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.DiagnosisOutcome
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ExecuTorchClassifier(private val context: Context) : Classifier {

    companion object {
        private const val TAG = "ExecuTorchClassifier"
        const val DEFAULT_INPUT_SIZE = 512
        const val MODEL_OUTPUT_SIZE = VisionMath.MODEL_OUTPUT_SIZE
    }

    private val fakeClassifier = FakeClassifier()

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initDeferred = CompletableDeferred<Boolean>()

    @Volatile
    private var module: Module? = null

    private var labels: List<String> = emptyList()
    private var inputSize: Int = DEFAULT_INPUT_SIZE
    private var mean: FloatArray = floatArrayOf(0.485f, 0.456f, 0.406f)
    private var std: FloatArray = floatArrayOf(0.229f, 0.224f, 0.225f)
    var temperature: Float = 1.7817543f
        private set

    // thresholds.json is the SINGLE SOURCE OF TRUTH (Task V2 & Task V7).
    // Required keys: temperature and confidence_threshold.
    // energy_threshold is optional (if absent, energy check is OFF).
    // Absolutely NO hard-coded numeric threshold fallbacks (Rule H11 & V7).
    var defaultThreshold: Float? = null
        private set
    var energyThreshold: Double? = null
        private set
    var cropMassThreshold: Float = 0.60f
        private set
    var marginThreshold: Float = 0.25f
        private set
    var borderlineEnergy: Double = -9.0
        private set
    var autoCropMassThreshold: Float = 0.60f
        private set
    var autoMarginThreshold: Float = 0.25f
        private set
    val classThresholds: MutableMap<String, Float> = mutableMapOf()
    val unsupportedClasses: MutableSet<String> = mutableSetOf()
    var isThresholdsLoaded: Boolean = false
        private set

    init {
        // Load assets and ExecuTorch model once in a background thread and keep them in memory
        scope.launch {
            loadConfigAndModel()
        }
    }

    private fun loadConfigAndModel() {
        try {
            loadLabels()
            loadPreprocessConfig()
            loadThresholds()
            loadSupportedClasses()
            if (!isEmulator()) {
                initializeModule()
            } else {
                Log.i(TAG, "Running on emulator: skipping native ExecuTorch vision module load (Rule H8).")
            }
            initDeferred.complete(isEmulator() || module != null)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed in background thread", e)
            initDeferred.complete(false)
        }
    }

    private fun loadLabels() {
        try {
            context.assets.open("labels.json").use { stream ->
                val text = stream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(text)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i).trim())
                }
                labels = list
                Log.i(TAG, "Loaded ${labels.size} labels from labels.json")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load labels.json", e)
        }
    }

    private fun loadPreprocessConfig() {
        try {
            val names = context.assets.list("") ?: emptyArray()
            if ("preprocess.json" in names) {
                context.assets.open("preprocess.json").use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    if (json.has("input_size")) inputSize = json.getInt("input_size")
                    if (json.has("temperature")) temperature = json.getDouble("temperature").toFloat()

                    if (json.has("mean")) {
                        val mArr = json.getJSONArray("mean")
                        mean = FloatArray(mArr.length()) { mArr.getDouble(it).toFloat() }
                    }
                    if (json.has("std")) {
                        val sArr = json.getJSONArray("std")
                        std = FloatArray(sArr.length()) { sArr.getDouble(it).toFloat() }
                    }
                }
                Log.i(TAG, "Loaded preprocess.json: size=$inputSize, temp=$temperature")
            }
        } catch (e: Exception) {
            Log.w(TAG, "preprocess.json not loaded, using defaults", e)
        }
    }

    /**
     * Parses thresholds from JSON string. Returns true if valid, false otherwise.
     * Enforces thresholds.json as the single source of truth without numeric fallbacks.
     * Required keys: temperature and confidence_threshold.
     * energy_threshold is optional (if absent, energy check is OFF).
     */
    fun parseThresholds(jsonText: String): Boolean {
        return try {
            val json = JSONObject(jsonText)
            if (!json.has("temperature") || !json.has("confidence_threshold")) {
                Log.e(TAG, "FATAL: thresholds.json missing required temperature or confidence_threshold!")
                isThresholdsLoaded = false
                defaultThreshold = null
                energyThreshold = null
                return false
            }
            defaultThreshold = json.getDouble("confidence_threshold").toFloat()
            temperature = json.getDouble("temperature").toFloat()

            // energy_threshold is optional; document that it is off when absent
            energyThreshold = if (json.has("energy_threshold")) {
                json.getDouble("energy_threshold")
            } else {
                null
            }

            // Task P4 & Task AUTO thresholds
            cropMassThreshold = if (json.has("crop_mass_threshold")) json.getDouble("crop_mass_threshold").toFloat() else 0.60f
            marginThreshold = if (json.has("margin_threshold")) json.getDouble("margin_threshold").toFloat() else 0.25f
            borderlineEnergy = if (json.has("borderline_energy")) json.getDouble("borderline_energy") else -9.0
            autoCropMassThreshold = if (json.has("auto_crop_mass_threshold")) json.getDouble("auto_crop_mass_threshold").toFloat() else cropMassThreshold
            autoMarginThreshold = if (json.has("auto_margin_threshold")) json.getDouble("auto_margin_threshold").toFloat() else marginThreshold

            classThresholds.clear()
            if (json.has("class_thresholds")) {
                val ct = json.getJSONObject("class_thresholds")
                val keys = ct.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    classThresholds[k] = ct.getDouble(k).toFloat()
                }
            }
            isThresholdsLoaded = true
            Log.i(TAG, "Successfully loaded thresholds.json: default=$defaultThreshold, temp=$temperature, energy=$energyThreshold, classes=${classThresholds.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Failed to parse thresholds.json", e)
            isThresholdsLoaded = false
            defaultThreshold = null
            energyThreshold = null
            false
        }
    }

    private fun loadThresholds() {
        try {
            val names = context.assets.list("") ?: emptyArray()
            if ("thresholds.json" in names) {
                context.assets.open("thresholds.json").use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    parseThresholds(text)
                }
            } else {
                Log.e(TAG, "FATAL: thresholds.json is missing from assets! Single source of truth violated.")
                isThresholdsLoaded = false
                defaultThreshold = null
                energyThreshold = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Error loading thresholds.json from assets", e)
            isThresholdsLoaded = false
            defaultThreshold = null
            energyThreshold = null
        }
    }

    /**
     * Parses supported_classes.json to identify blocked / unsupported classes.
     */
    fun parseSupportedClasses(jsonText: String): Boolean {
        return try {
            val json = JSONObject(jsonText)
            unsupportedClasses.clear()
            val targetObj = if (json.has("classes")) {
                val c = json.get("classes")
                if (c is JSONObject) c else null
            } else json

            if (targetObj != null) {
                val keys = targetObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val entry = targetObj.getJSONObject(k)
                    val supported = if (entry.has("supported")) entry.getBoolean("supported") else true
                    if (!supported && entry.has("class_id")) {
                        unsupportedClasses.add(entry.getString("class_id"))
                    }
                }
            } else if (json.has("classes")) {
                val arr = json.getJSONArray("classes")
                for (i in 0 until arr.length()) {
                    val entry = arr.getJSONObject(i)
                    val supported = if (entry.has("supported")) entry.getBoolean("supported") else true
                    if (!supported && entry.has("class_id")) {
                        unsupportedClasses.add(entry.getString("class_id"))
                    }
                }
            }
            Log.i(TAG, "Loaded supported_classes.json: unsupportedCount=${unsupportedClasses.size} (${unsupportedClasses.joinToString()})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse supported_classes.json", e)
            false
        }
    }

    private fun loadSupportedClasses() {
        try {
            val names = context.assets.list("") ?: emptyArray()
            if ("supported_classes.json" in names) {
                context.assets.open("supported_classes.json").use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    parseSupportedClasses(text)
                }
            } else {
                Log.w(TAG, "supported_classes.json not found in assets, defaulting to empty unsupported set")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading supported_classes.json", e)
        }
    }

    private fun initializeModule() {
        try {
            val candidateAssets = listOf("models/vision.pte", "vision.pte", "vision/vision.pte")
            var resolvedAsset: String? = null

            for (candidate in candidateAssets) {
                try {
                    context.assets.open(candidate).close()
                    resolvedAsset = candidate
                    break
                } catch (_: Exception) {}
            }

            val modelFile = File(context.filesDir, "vision.pte")
            val versionFile = File(context.filesDir, "vision_model_version.txt")
            val currentModelVersion = "20261122_15class_tr4"
            if (resolvedAsset != null) {
                try {
                    val needsExtract = !modelFile.exists() ||
                            !versionFile.exists() ||
                            versionFile.readText().trim() != currentModelVersion ||
                            modelFile.length() != 16_931_132L

                    if (needsExtract) {
                        context.assets.open(resolvedAsset).use { input ->
                            FileOutputStream(modelFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        versionFile.writeText(currentModelVersion)
                        Log.i(TAG, "Extracted $resolvedAsset to ${modelFile.absolutePath} (${modelFile.length()} bytes, version=$currentModelVersion)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to copy $resolvedAsset", e)
                }
            }

            val targetPath = if (modelFile.exists() && modelFile.length() > 0) {
                modelFile.absolutePath
            } else {
                val devPath = File("/data/local/tmp/vision.pte")
                if (devPath.exists()) devPath.absolutePath else null
            }

            Log.e(TAG, "initializeModule: resolvedAsset=$resolvedAsset, modelFile=${modelFile.absolutePath} (len=${modelFile.length()}), targetPath=$targetPath")

            if (targetPath != null) {
                try {
                    module = Module.load(targetPath, Module.LOAD_MODE_FILE)
                    Log.e(TAG, "Successfully loaded ExecuTorch vision model from $targetPath: module=$module")
                } catch (t: Throwable) {
                    Log.e(TAG, "FATAL: Module.load($targetPath) threw: ${t.message}", t)
                    module = null
                }
            } else {
                Log.e(TAG, "No vision.pte available. Strict fallback to uncertain diagnosis (Rule H11).")
                module = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "ExecuTorch vision module load failed.", e)
            module = null
        }
    }

    private var reusableTensorBuffer: java.nio.FloatBuffer? = null
    private val forwardMutex = kotlinx.coroutines.sync.Mutex()

    @Synchronized
    fun getReusableBuffer(): java.nio.FloatBuffer {
        var buf = reusableTensorBuffer
        if (buf == null) {
            buf = java.nio.ByteBuffer.allocateDirect(3 * inputSize * inputSize * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer()
            reusableTensorBuffer = buf
        }
        return buf
    }

    override suspend fun classify(bitmap: Bitmap): ClassifierResult {
        return classify(bitmap, null, null)
    }

    override suspend fun classify(bitmap: Bitmap, cropHint: String?): ClassifierResult {
        return classify(bitmap, cropHint, null)
    }

    override suspend fun classify(
        bitmap: Bitmap,
        cropHint: String?,
        metadata: ImageAnalysisMetadata?
    ): ClassifierResult = withContext(Dispatchers.Default) {
        val preprocessed = ImagePreprocessor.prepare(
            bitmap = bitmap,
            exifRotationDegrees = metadata?.exifOrientation ?: 0,
            reusableBuffer = getReusableBuffer()
        )
        classify(preprocessed, cropHint, metadata)
    }

    override suspend fun classify(
        preprocessed: ImagePreprocessor.PreprocessedImage,
        cropHint: String?,
        metadata: ImageAnalysisMetadata?
    ): ClassifierResult = withContext(Dispatchers.Default) {
        val startTime = SystemClock.elapsedRealtime()

        if (!initDeferred.isCompleted) {
            initDeferred.await()
        }

        // Single Source of Truth Enforcement (Task V2 & Task V7):
        // If thresholds.json is missing or invalid, NEVER guess or fall back to silent numbers.
        // Classifier must return "Can't tell" for every photo and log an error.
        val curDefaultThresh = defaultThreshold
        val curEnergyThresh = energyThreshold
        if (!isThresholdsLoaded || curDefaultThresh == null) {
            Log.e(TAG, "classify() aborted: thresholds.json is missing or invalid! Single source of truth violated. Returning 'Can't tell'.")
            val elapsed = SystemClock.elapsedRealtime() - startTime
            return@withContext ClassifierResult(
                classId = "unknown",
                displayName = "Not sure - ask an extension officer",
                confidence = 0.0f,
                severityBand = SeverityBand.MILD,
                certaintyBand = CertaintyBand.NOT_SURE,
                lesionFraction = 0.0f,
                inferenceTimeMs = elapsed,
                memoryUsedMb = getProcessMemoryMb(),
                isDemoMode = false,
                top3 = emptyList(),
                selectedCrop = cropHint,
                cropMass = 1.0f
            )
        }

        // Rule H8: Emulators cannot run 4-bit/arm64 kernels -> delegate to FakeClassifier with visible demo banner
        if (isEmulator()) {
            Log.e(TAG, "Running on emulator. Delegating to FakeClassifier (Rule H8).")
            return@withContext fakeClassifier.classify(preprocessed.bitmap512, cropHint)
        }

        Log.e(TAG, "classify() called: cropHint=$cropHint, bitmap=${preprocessed.bitmap512.width}x${preprocessed.bitmap512.height}, moduleNull=${module == null}, labelsCount=${labels.size}")

        // Rule H11 & T1: If model is missing or fails to load, NEVER guess a disease.
        if (module == null || labels.isEmpty()) {
            Log.e(TAG, "classify() EARLY EXIT: module is null (${module == null}) or labels empty (${labels.size})")
            val elapsed = SystemClock.elapsedRealtime() - startTime
            return@withContext ClassifierResult(
                classId = "unknown",
                displayName = "Not sure - ask an extension officer",
                confidence = 0.0f,
                severityBand = SeverityBand.MILD,
                certaintyBand = CertaintyBand.NOT_SURE,
                lesionFraction = 0.0f,
                inferenceTimeMs = elapsed,
                memoryUsedMb = getProcessMemoryMb(),
                isDemoMode = false,
                top3 = emptyList(),
                selectedCrop = cropHint,
                cropMass = 1.0f
            )
        }

        try {
            // Forward pass reusing native tensor buffer (Rule P2.3) protected by forwardMutex against concurrent native execution
            val outputs = forwardMutex.withLock {
                val inputTensor = Tensor.fromBlob(
                    preprocessed.tensorBuffer,
                    longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
                )
                module!!.forward(EValue.from(inputTensor))
            }
            val outputTensor = outputs[0].toTensor()
            val rawLogits = outputTensor.dataAsFloatArray

            // 3. Evaluate model outputs against labels.json (Task AUTO / Picker)
            val scaledLogits = VisionMath.applyTemperature(rawLogits.take(labels.size).toFloatArray(), temperature)
            val freeEnergy = VisionMath.computeFreeEnergy(scaledLogits, temperature)
            val isFilteredCrop = cropHint != null && cropHint.isNotBlank() && cropHint != "all" && cropHint != "auto"
            val globalProbs = VisionMath.softmax(scaledLogits)
            val cropMasses = com.cropdoctor.domain.CropMapping.computeCropMasses(labels, globalProbs)

            // Gate 1: Leaf Validator (Task P1 & P4)
            val gate1Validation = if (metadata != null) {
                LeafValidationResult(
                    isLikelyCrop = metadata.leafValidatorPass,
                    plantPixelFraction = metadata.leafValidatorFraction,
                    reason = metadata.leafValidatorReason
                )
            } else {
                preprocessed.leafValidation
            }
            val gate1Pass = gate1Validation.isLikelyCrop

            val (eval, effectiveCropMass, detectedCrop) = if (isFilteredCrop) {
                val maskResult = VisionMath.maskAndRenormalizeWithMass(labels, scaledLogits, cropHint)
                val probabilities = maskResult.probabilities
                val cropMass = maskResult.cropMass
                val res = VisionMath.evaluateDiagnosis(
                    labels = labels,
                    probabilities = probabilities,
                    thresholds = classThresholds,
                    defaultThreshold = curDefaultThresh,
                    freeEnergy = freeEnergy,
                    energyThreshold = curEnergyThresh,
                    unsupportedClasses = unsupportedClasses,
                    cropMass = cropMass,
                    cropMassThreshold = cropMassThreshold,
                    marginThreshold = marginThreshold,
                    borderlineEnergy = borderlineEnergy,
                    gate1Pass = gate1Pass,
                    leafValidatorReason = gate1Validation.reason
                )
                Triple(res, cropMass, cropHint)
            } else {
                val res = VisionMath.evaluateDiagnosisAuto(
                    labels = labels,
                    probabilities = globalProbs,
                    thresholds = classThresholds,
                    defaultThreshold = curDefaultThresh,
                    freeEnergy = freeEnergy,
                    energyThreshold = curEnergyThresh,
                    cropMassThreshold = autoCropMassThreshold,
                    marginThreshold = autoMarginThreshold,
                    gate1Pass = gate1Pass,
                    leafValidatorReason = gate1Validation.reason
                )
                val top1Class = res.top3.firstOrNull()?.classId
                val top1Crop = if (top1Class != null) com.cropdoctor.domain.CropMapping.getCropForClass(top1Class) else null
                val mass = if (top1Crop != null) cropMasses[top1Crop] ?: 0.0f else 0.0f
                Triple(res, mass, top1Crop)
            }

            // CROPDOC_DECISION Gate Diagnostic Logging (Task P1 & P4)
            val bitmapConfig = preprocessed.bitmap512.config?.name ?: "UNKNOWN"
            val colorSpaceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                preprocessed.bitmap512.colorSpace?.name ?: "UNKNOWN"
            } else {
                "sRGB"
            }

            // Gate 2: Energy value vs energy_threshold
            val isOOD = if (curEnergyThresh != null) freeEnergy > curEnergyThresh else false

            // Gate 3: Top-1 class and calibrated probability versus confidence_threshold
            val topK = eval.top3
            val top1 = topK.firstOrNull()
            val requiredThreshold = if (top1 != null) {
                classThresholds[top1.classId] ?: curDefaultThresh
            } else {
                curDefaultThresh
            }
            val isBelowThreshold = top1 == null || top1.probability < requiredThreshold

            // Gate 4: Non-crop class (weeds_not_a_crop, other_not_a_crop) & unsupported classes
            val isWeeds = top1 != null && (top1.classId == "weeds_not_a_crop" || top1.classId == "other_not_a_crop")
            val isUnsupported = top1 != null && unsupportedClasses.contains(top1.classId)
            val gate4Pass = !isWeeds && !isUnsupported

            val gate1Str = if (gate1Pass) {
                "PASS(frac=${String.format(Locale.US, "%.3f", gate1Validation.plantPixelFraction)})"
            } else {
                "FAIL(frac=${String.format(Locale.US, "%.3f", gate1Validation.plantPixelFraction)}, reason=${gate1Validation.reason})"
            }

            val gate2Str = if (curEnergyThresh == null) {
                "OFF(energy=${String.format(Locale.US, "%.4f", freeEnergy)} vs null)"
            } else if (!isOOD) {
                "PASS(energy=${String.format(Locale.US, "%.4f", freeEnergy)} vs thresh=${String.format(Locale.US, "%.4f", curEnergyThresh)})"
            } else {
                "FAIL(energy=${String.format(Locale.US, "%.4f", freeEnergy)} vs thresh=${String.format(Locale.US, "%.4f", curEnergyThresh)})"
            }

            val gate3Str = if (!isBelowThreshold) {
                "PASS(class=${top1?.classId ?: "none"}, prob=${String.format(Locale.US, "%.4f", top1?.probability ?: 0f)} vs thresh=${String.format(Locale.US, "%.4f", requiredThreshold)})"
            } else {
                "FAIL(class=${top1?.classId ?: "none"}, prob=${String.format(Locale.US, "%.4f", top1?.probability ?: 0f)} vs thresh=${String.format(Locale.US, "%.4f", requiredThreshold)})"
            }

            val gate4Str = if (gate4Pass) "PASS" else "FAIL(weeds=$isWeeds, unsupported=$isUnsupported)"

            val logDecisionLine = buildString {
                append("CROPDOC_DECISION: ")
                append("source=${metadata?.source ?: "camera"} ")
                append("orig=${metadata?.origWidth ?: preprocessed.origWidth}x${metadata?.origHeight ?: preprocessed.origHeight} ")
                append("exif=${metadata?.exifOrientation ?: 0} ")
                append("config=$bitmapConfig ")
                append("colorSpace=$colorSpaceName ")
                append("downscale=${inputSize}x${inputSize} | ")
                append("gate1_leafValidator=$gate1Str ")
                append("gate2_energy=$gate2Str ")
                append("gate3_confidence=$gate3Str ")
                append("gate4_weeds=$gate4Str | ")
                append("cropMass=${String.format(Locale.US, "%.4f", effectiveCropMass)} ")
                append("margin=${String.format(Locale.US, "%.4f", eval.margin)} | ")
                append("outcome=${eval.outcome} code=${eval.outcomeReason}")
            }
            Log.i("CROPDOC_DECISION", logDecisionLine)

            Log.e(TAG, "classify(): eval result -> outcome=${eval.outcome}, classId=${eval.classId}, displayName=${eval.displayName}, conf=${eval.confidence}, cropMass=$effectiveCropMass, margin=${eval.margin}, top3=${eval.top3.map { "${it.classId}:${String.format("%.3f", it.probability)}" }}")

            val elapsed = SystemClock.elapsedRealtime() - startTime
            val memoryMb = getProcessMemoryMb()

            // 4. Lesion Fraction Estimation (only for confident disease diagnosis)
            val lesionFraction = if (eval.outcome != DiagnosisOutcome.DIAGNOSE || eval.classId.endsWith("_healthy") || eval.classId == "unknown") {
                0.0f
            } else {
                LesionAreaEstimator.estimateLesionFraction(preprocessed.bitmap512)
            }

            // 5. Severity Band (Rule H5: safety decisions in plain code; MILD for non-diagnoses)
            val severityBand = if (eval.outcome != DiagnosisOutcome.DIAGNOSE || eval.classId == "unknown" || eval.classId.endsWith("_healthy")) {
                SeverityBand.MILD
            } else {
                RulesEngine.calculateSeverity(lesionFraction, eval.classId)
            }

            ClassifierResult(
                classId = eval.classId,
                displayName = eval.displayName,
                confidence = eval.confidence,
                severityBand = severityBand,
                certaintyBand = eval.certaintyBand,
                lesionFraction = lesionFraction,
                inferenceTimeMs = elapsed,
                memoryUsedMb = memoryMb,
                isDemoMode = false,
                top3 = eval.top3,
                selectedCrop = if (isFilteredCrop) cropHint else null,
                cropMass = effectiveCropMass,
                outcome = eval.outcome,
                outcomeReason = eval.outcomeReason,
                margin = eval.margin,
                freeEnergy = freeEnergy,
                detectedCrop = detectedCrop,
                isAutoCrop = !isFilteredCrop,
                cropMassPerCrop = cropMasses
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Vision inference error caught: ${e.message}", e)
            val elapsed = SystemClock.elapsedRealtime() - startTime
            ClassifierResult(
                classId = "unknown",
                displayName = "Not sure - ask an extension officer",
                confidence = 0.0f,
                severityBand = SeverityBand.MILD,
                certaintyBand = CertaintyBand.NOT_SURE,
                lesionFraction = 0.0f,
                inferenceTimeMs = elapsed,
                memoryUsedMb = getProcessMemoryMb(),
                isDemoMode = false,
                top3 = emptyList(),
                selectedCrop = cropHint,
                cropMass = 1.0f,
                outcome = DiagnosisOutcome.CANT_TELL,
                outcomeReason = "EXCEPTION: ${e.message}",
                detectedCrop = cropHint,
                isAutoCrop = cropHint == null,
                cropMassPerCrop = emptyMap()
            )
        }
    }

    private fun getProcessMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    /**
     * Preprocesses bitmap into Tensor matching PyTorch transforms exactly:
     * Planar [1, 3, H, W], RGB channel order, normalized by mean and std.
     */
    internal fun preprocessBitmap(bitmap: Bitmap): Tensor {
        val width = inputSize
        val height = inputSize
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val directBuffer = Tensor.allocateFloatBuffer(3 * width * height)
        val m = mean
        val s = std

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            directBuffer.put(i, (r - m[0]) / s[0])
            directBuffer.put(width * height + i, (g - m[1]) / s[1])
            directBuffer.put(2 * width * height + i, (b - m[2]) / s[2])
        }
        directBuffer.rewind()

        return Tensor.fromBlob(directBuffer, longArrayOf(1, 3, width.toLong(), height.toLong()))
    }

    override fun close() {
        module = null
        reusableTensorBuffer = null
    }
}
