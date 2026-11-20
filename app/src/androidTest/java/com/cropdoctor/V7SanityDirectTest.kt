package com.cropdoctor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.vision.VisionMath
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class V7SanityDirectTest {

    private val TAG = "V7SanityDirectTest"

    data class TestCase(
        val fileName: String,
        val trueLabel: String
    )

    @Test
    fun test5ValidationImagesDirectExecuTorch() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Load labels.json
        val labelsStream = context.assets.open("labels.json")
        val labelsJson = labelsStream.bufferedReader().use { it.readText() }
        val labelsArray = JSONArray(labelsJson)
        val labels = (0 until labelsArray.length()).map { labelsArray.getString(it) }

        // 2. Load thresholds.json
        val threshStream = context.assets.open("thresholds.json")
        val threshJson = threshStream.bufferedReader().use { it.readText() }
        val threshObj = JSONObject(threshJson)
        val temperature = threshObj.getDouble("temperature").toFloat()

        // 3. Extract and load vision.pte
        val modelFile = File(context.filesDir, "vision.pte")
        if (!modelFile.exists() || modelFile.length() != 16_956_752L) {
            context.assets.open("models/vision.pte").use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        Log.i(TAG, "Loading ExecuTorch module from ${modelFile.absolutePath} (${modelFile.length()} bytes)...")
        val module = Module.load(modelFile.absolutePath, Module.LOAD_MODE_FILE)
        assertNotNull("ExecuTorch module should load successfully", module)

        val testCases = listOf(
            TestCase("/data/local/tmp/v7_sanity/sample1_rice.jpg", "rice_leaf_blast"),
            TestCase("/data/local/tmp/v7_sanity/sample2_cotton.jpg", "cotton_bacterial_blight"),
            TestCase("/data/local/tmp/v7_sanity/sample3_chilli.jpg", "chilli_leaf_curl_virus"),
            TestCase("/data/local/tmp/v7_sanity/sample4_tomato.jpg", "tomato_yellow_leaf_curl_virus"),
            TestCase("/data/local/tmp/v7_sanity/sample5_maize.jpg", "maize_northern_leaf_blight")
        )

        val resultsArray = JSONArray()

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        val inputSize = 512

        for ((index, tc) in testCases.withIndex()) {
            val file = File(tc.fileName)
            assertTrue("Test image must exist: ${tc.fileName}", file.exists())

            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull("Failed to decode image: ${tc.fileName}", rawBitmap)

            // Ensure input image to vision model is 512x512
            val bitmap512 = Bitmap.createScaledBitmap(rawBitmap, inputSize, inputSize, true)
            assertTrue("Bitmap width must be 512", bitmap512.width == 512)
            assertTrue("Bitmap height must be 512", bitmap512.height == 512)

            // Preprocess bitmap into float buffer planar [1, 3, 512, 512]
            val pixels = IntArray(inputSize * inputSize)
            bitmap512.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            val directBuffer = Tensor.allocateFloatBuffer(3 * inputSize * inputSize)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f

                directBuffer.put(i, (r - mean[0]) / std[0])
                directBuffer.put(inputSize * inputSize + i, (g - mean[1]) / std[1])
                directBuffer.put(2 * inputSize * inputSize + i, (b - mean[2]) / std[2])
            }
            directBuffer.rewind()
            val inputTensor = Tensor.fromBlob(directBuffer, longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()))

            val tStart = SystemClock.elapsedRealtime()
            val outputs = module.forward(EValue.from(inputTensor))
            val inferenceTimeMs = SystemClock.elapsedRealtime() - tStart

            val outputTensor = outputs[0].toTensor()
            val rawLogits = outputTensor.dataAsFloatArray

            // Calibration & Softmax
            val scaledLogits = VisionMath.applyTemperature(rawLogits.take(labels.size).toFloatArray(), temperature)
            val probabilities = VisionMath.softmax(scaledLogits)

            var top1Idx = 0
            var top1Prob = probabilities[0]
            for (i in 1 until probabilities.size) {
                if (probabilities[i] > top1Prob) {
                    top1Prob = probabilities[i]
                    top1Idx = i
                }
            }
            val top1Label = labels[top1Idx]

            Log.i(TAG, "[$index] True: ${tc.trueLabel} | Top1: $top1Label | Prob: $top1Prob | Time: ${inferenceTimeMs}ms")

            val obj = JSONObject().apply {
                put("index", index + 1)
                put("file_name", tc.fileName)
                put("true_label", tc.trueLabel)
                put("top1_label", top1Label)
                put("probability", top1Prob.toDouble())
                put("inference_time_ms", inferenceTimeMs)
            }
            resultsArray.put(obj)
        }

        val outFile = File(context.filesDir, "phone_direct_results.json")
        outFile.writeText(resultsArray.toString(2))
        Log.i(TAG, "Wrote direct phone results to ${outFile.absolutePath}:\n${resultsArray.toString(2)}")
    }
}
