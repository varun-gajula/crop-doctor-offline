package com.cropdoctor.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.CertaintyCalculator
import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.domain.SeverityCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.exp

class ExecuTorchClassifier(private val context: Context) : Classifier {

    private val TAG = "ExecuTorchClassifier"
    private var module: Module? = null
    private var labels: List<String> = emptyList()

    init {
        loadLabels()
        initializeModule()
    }

    private fun loadLabels() {
        try {
            context.assets.open("labels.json").use { stream ->
                val text = stream.bufferedReader().use { it.readText() }
                labels = text.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load labels.json", e)
        }
    }

    private fun initializeModule() {
        try {
            val assetName = "models/vision.pte"
            val modelFile = File(context.filesDir, "vision.pte")
            if (!modelFile.exists()) {
                try {
                    context.assets.open(assetName).use { input ->
                        FileOutputStream(modelFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "vision.pte not found in assets")
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

        // Rule H11 & T1: If model is missing or fails to load, NEVER guess a disease.
        if (module == null) {
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
                isDemoMode = false
            )
        }

        try {
            val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val inputTensor = preprocessBitmap(resized)

            val outputs = module!!.forward(EValue.from(inputTensor))
            val outputTensor = outputs[0].toTensor()
            val logits = outputTensor.dataAsFloatArray

            // Softmax
            val probabilities = softmax(logits)
            var topIdx = 0
            var maxProb = probabilities[0]
            for (i in 1 until probabilities.size) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    topIdx = i
                }
            }

            val classId = if (topIdx < labels.size) labels[topIdx] else "unknown"
            val elapsed = SystemClock.elapsedRealtime() - startTime
            val memoryMb = getProcessMemoryMb()

            if (classId == "unknown") {
                return@withContext ClassifierResult(
                    classId = "unknown",
                    displayName = "Not sure - ask an extension officer",
                    confidence = maxProb,
                    severityBand = SeverityBand.MILD,
                    certaintyBand = CertaintyBand.NOT_SURE,
                    lesionFraction = 0.0f,
                    inferenceTimeMs = elapsed,
                    memoryUsedMb = memoryMb,
                    isDemoMode = false
                )
            }

            // Lesion fraction: estimated via color heuristics (Rule H11), zero for healthy classes
            val lesionFraction = if (classId.endsWith("_healthy")) {
                0.0f
            } else {
                LesionAreaEstimator.estimateLesionFraction(bitmap)
            }

            val severityBand = SeverityCalculator.calculate(lesionFraction, classId)
            val certaintyBand = CertaintyCalculator.calculate(maxProb)

            ClassifierResult(
                classId = classId,
                displayName = classId.replace("_", " ").replaceFirstChar { it.uppercase() },
                confidence = maxProb,
                severityBand = severityBand,
                certaintyBand = certaintyBand,
                lesionFraction = lesionFraction,
                inferenceTimeMs = elapsed,
                memoryUsedMb = memoryMb,
                isDemoMode = false
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
                isDemoMode = false
            )
        }
    }

    private fun getProcessMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    private fun preprocessBitmap(bitmap: Bitmap): Tensor {
        val width = 224
        val height = 224
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatBuffer = FloatArray(3 * width * height)
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            floatBuffer[i] = (r - mean[0]) / std[0]
            floatBuffer[width * height + i] = (g - mean[1]) / std[1]
            floatBuffer[2 * width * height + i] = (b - mean[2]) / std[2]
        }

        return Tensor.fromBlob(floatBuffer, longArrayOf(1, 3, 224, 224))
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var max = logits[0]
        for (v in logits) if (v > max) max = v
        val exp = FloatArray(logits.size)
        var sum = 0.0f
        for (i in logits.indices) {
            exp[i] = exp(logits[i] - max)
            sum += exp[i]
        }
        for (i in exp.indices) {
            exp[i] /= sum
        }
        return exp
    }

    override fun close() {
        module = null
    }
}
