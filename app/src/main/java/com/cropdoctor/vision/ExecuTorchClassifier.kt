package com.cropdoctor.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.ClassifierResult
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
        const val DEFAULT_TEMPERATURE = 1.15f
        const val DEFAULT_ENERGY_THRESHOLD = -1.5
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
    private var temperature: Float = DEFAULT_TEMPERATURE
    private var energyThreshold: Double = DEFAULT_ENERGY_THRESHOLD
    private var defaultThreshold: Float = 0.40f
    private val classThresholds: MutableMap<String, Float> = mutableMapOf()

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
                    if (json.has("energy_threshold")) energyThreshold = json.getDouble("energy_threshold")

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

    private fun loadThresholds() {
        try {
            val names = context.assets.list("") ?: emptyArray()
            if ("thresholds.json" in names) {
                context.assets.open("thresholds.json").use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    if (json.has("default_threshold")) {
                        defaultThreshold = json.getDouble("default_threshold").toFloat()
                    }
                    if (json.has("temperature")) {
                        temperature = json.getDouble("temperature").toFloat()
                    }
                    if (json.has("energy_threshold")) {
                        energyThreshold = json.getDouble("energy_threshold")
                    }
                    if (json.has("class_thresholds")) {
                        val ct = json.getJSONObject("class_thresholds")
                        val keys = ct.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            classThresholds[k] = ct.getDouble(k).toFloat()
                        }
                    }
                }
                Log.i(TAG, "Loaded thresholds.json: ${classThresholds.size} class thresholds")
            }
        } catch (e: Exception) {
            Log.w(TAG, "thresholds.json not loaded, using defaults", e)
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

            if (targetPath != null) {
                module = Module.load(targetPath)
                Log.i(TAG, "Successfully loaded ExecuTorch vision model from $targetPath")
            } else {
                Log.w(TAG, "No vision.pte available. Strict fallback to uncertain diagnosis (Rule H11).")
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

        // Rule H8: Emulators cannot run 4-bit/arm64 kernels -> delegate to FakeClassifier with visible demo banner
        if (isEmulator()) {
            Log.i(TAG, "Running on emulator. Delegating to FakeClassifier (Rule H8).")
            return@withContext fakeClassifier.classify(bitmap, cropHint)
        }

        // Rule H11 & T1: If model is missing or fails to load, NEVER guess a disease.
        if (module == null || labels.isEmpty()) {
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
            val outputs = module!!.forward(EValue.from(inputTensor))
            val outputTensor = outputs[0].toTensor()
            val rawLogits = outputTensor.dataAsFloatArray

            // 3. Temperature Scaling
            val scaledLogits = VisionMath.applyTemperature(rawLogits, temperature)

            // 4. Free Energy Calculation for OOD Rejection
            val freeEnergy = VisionMath.computeFreeEnergy(scaledLogits, temperature)

            // 5. Crop Chip Masking & Re-normalization
            val probabilities = VisionMath.maskAndRenormalize(labels, scaledLogits, cropHint)

            // 6. Diagnosis Evaluation with Confidence Thresholds & Top-3
            val eval = VisionMath.evaluateDiagnosis(
                labels = labels,
                probabilities = probabilities,
                thresholds = classThresholds,
                defaultThreshold = defaultThreshold,
                freeEnergy = freeEnergy,
                energyThreshold = energyThreshold
            )

            val elapsed = SystemClock.elapsedRealtime() - startTime
            val memoryMb = getProcessMemoryMb()

            // 7. Lesion Fraction Estimation
            val lesionFraction = if (eval.classId.endsWith("_healthy") || eval.classId == "unknown") {
                0.0f
            } else {
                LesionAreaEstimator.estimateLesionFraction(bitmap)
            }

            // 8. Severity Band: learned multi-task head if available; otherwise lesion area calculation
            val severityBand = if (outputs.size > 1 && eval.classId != "unknown" && !eval.classId.endsWith("_healthy")) {
                val sevTensor = outputs[1].toTensor()
                val sevLogits = sevTensor.dataAsFloatArray
                var topSevIdx = 0
                var maxSevLogit = sevLogits[0]
                for (i in 1 until sevLogits.size) {
                    if (sevLogits[i] > maxSevLogit) {
                        maxSevLogit = sevLogits[i]
                        topSevIdx = i
                    }
                }
                when (topSevIdx) {
                    0 -> SeverityBand.MILD
                    1 -> SeverityBand.MODERATE
                    else -> SeverityBand.SEVERE
                }
            } else {
                SeverityCalculator.calculate(lesionFraction, eval.classId)
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
        } catch (e: Exception) {
            Log.e(TAG, "Vision inference error", e)
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
    private fun preprocessBitmap(bitmap: Bitmap): Tensor {
        val width = inputSize
        val height = inputSize
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatBuffer = FloatArray(3 * width * height)
        val m = mean
        val s = std

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            floatBuffer[i] = (r - m[0]) / s[0]
            floatBuffer[width * height + i] = (g - m[1]) / s[1]
            floatBuffer[2 * width * height + i] = (b - m[2]) / s[2]
        }

        return Tensor.fromBlob(floatBuffer, longArrayOf(1, 3, width.toLong(), height.toLong()))
    }

    override fun close() {
        module = null
    }
}
