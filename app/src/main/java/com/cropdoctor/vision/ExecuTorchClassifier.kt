package com.cropdoctor.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.domain.SeverityCalculator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream

class ExecuTorchClassifier(private val context: Context) : Classifier {

    companion object {
        private const val TAG = "ExecuTorchClassifier"
        const val DEFAULT_INPUT_SIZE = 320
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
    private var temperature: Float = 1.0f

    // thresholds.json is the SINGLE SOURCE OF TRUTH (Task V2).
    // Absolutely NO hard-coded numeric threshold fallbacks (Rule H11 & V2).
    var defaultThreshold: Float? = null
        private set
    var energyThreshold: Double? = null
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
     */
    fun parseThresholds(jsonText: String): Boolean {
        return try {
            val json = JSONObject(jsonText)
            if (!json.has("default_threshold") || !json.has("energy_threshold")) {
                Log.e(TAG, "FATAL: thresholds.json missing required default_threshold or energy_threshold!")
                isThresholdsLoaded = false
                defaultThreshold = null
                energyThreshold = null
                return false
            }
            defaultThreshold = json.getDouble("default_threshold").toFloat()
            energyThreshold = json.getDouble("energy_threshold")
            if (json.has("temperature")) {
                temperature = json.getDouble("temperature").toFloat()
            }
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
            Log.i(TAG, "Successfully loaded thresholds.json: default=$defaultThreshold, energy=$energyThreshold, classes=${classThresholds.size}")
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
            // Check both models/vision.pte and vision/vision.pte
            val candidateAssets = listOf("models/vision.pte", "vision/vision.pte")
            var resolvedAsset: String? = null

            for (candidate in candidateAssets) {
                try {
                    context.assets.open(candidate).close()
                    resolvedAsset = candidate
                    break
                } catch (_: Exception) {}
            }

            val modelFile = File(context.filesDir, "vision.pte")
            if (resolvedAsset != null) {
                try {
                    // Extract if not present or corrupt/incomplete (< 10 MB)
                    if (!modelFile.exists() || modelFile.length() < 10_000_000L) {
                        context.assets.open(resolvedAsset).use { input ->
                            FileOutputStream(modelFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.i(TAG, "Extracted $resolvedAsset to ${modelFile.absolutePath} (${modelFile.length()} bytes)")
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

    override suspend fun classify(bitmap: Bitmap): ClassifierResult {
        return classify(bitmap, null)
    }

    override suspend fun classify(bitmap: Bitmap, cropHint: String?): ClassifierResult = withContext(Dispatchers.Default) {
        val startTime = SystemClock.elapsedRealtime()

        // Wait for background initialization if not yet finished
        if (!initDeferred.isCompleted) {
            initDeferred.await()
        }

        // Single Source of Truth Enforcement (Task V2):
        // If thresholds.json is missing or invalid, NEVER guess or fall back to silent numbers.
        // Classifier must return "Can't tell" for every photo and log an error.
        val curDefaultThresh = defaultThreshold
        val curEnergyThresh = energyThreshold
        if (!isThresholdsLoaded || curDefaultThresh == null || curEnergyThresh == null) {
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
                top3 = emptyList()
            )
        }

        // Rule H8: Emulators cannot run 4-bit/arm64 kernels -> delegate to FakeClassifier with visible demo banner
        if (isEmulator()) {
            Log.e(TAG, "Running on emulator. Delegating to FakeClassifier (Rule H8).")
            return@withContext fakeClassifier.classify(bitmap, cropHint)
        }

        Log.e(TAG, "classify() called: cropHint=$cropHint, bitmap=${bitmap.width}x${bitmap.height}, moduleNull=${module == null}, labelsCount=${labels.size}")

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
                top3 = emptyList()
            )
        }

        try {
            // 1. Preprocessing matching training exactly
            val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            val inputTensor = preprocessBitmap(resized)

            // 2. ExecuTorch Forward Pass
            Log.e(TAG, "classify(): executing module.forward()...")
            val outputs = module!!.forward(EValue.from(inputTensor))
            Log.e(TAG, "classify(): forward pass returned ${outputs.size} outputs")
            val outputTensor = outputs[0].toTensor()
            val rawLogits = outputTensor.dataAsFloatArray
            Log.e(TAG, "classify(): rawLogits count=${rawLogits.size}, first 5=[${rawLogits.take(5).joinToString(", ")}]")

            // 3. Temperature Scaling
            val scaledLogits = VisionMath.applyTemperature(rawLogits, temperature)

            // 4. Free Energy Calculation for OOD Rejection
            val freeEnergy = VisionMath.computeFreeEnergy(scaledLogits, temperature)

            // 5. Crop Chip Masking & Re-normalization
            val probabilities = VisionMath.maskAndRenormalize(labels, scaledLogits, cropHint)
            Log.e(TAG, "classify(): freeEnergy=$freeEnergy, energyThreshold=$curEnergyThresh")

            // 6. Diagnosis Evaluation with Confidence Thresholds, Top-3 & Blocked Classes (Task V2)
            val eval = VisionMath.evaluateDiagnosis(
                labels = labels,
                probabilities = probabilities,
                thresholds = classThresholds,
                defaultThreshold = curDefaultThresh,
                freeEnergy = freeEnergy,
                energyThreshold = curEnergyThresh,
                unsupportedClasses = unsupportedClasses
            )
            Log.e(TAG, "classify(): eval result -> classId=${eval.classId}, displayName=${eval.displayName}, conf=${eval.confidence}, isOOD=${eval.isOutOfDistribution}, isBelowThresh=${eval.isBelowThreshold}, top3=${eval.top3.map { "${it.classId}:${String.format("%.3f", it.probability)}" }}")

            val elapsed = SystemClock.elapsedRealtime() - startTime
            val memoryMb = getProcessMemoryMb()

            // 7. Lesion Fraction Estimation
            val lesionFraction = if (eval.classId.endsWith("_healthy") || eval.classId == "unknown") {
                0.0f
            } else {
                LesionAreaEstimator.estimateLesionFraction(bitmap)
            }

            // 8. Severity Band: Decision D1 - The app uses ONLY the 25-class output.
            // Any learned severity head from the vision model is completely ignored.
            // Severity comes EXCLUSIVELY from LesionAreaEstimator and RulesEngine.
            val severityBand = if (eval.classId == "unknown" || eval.classId.endsWith("_healthy")) {
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
                top3 = eval.top3
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
                top3 = emptyList()
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
    }
}
