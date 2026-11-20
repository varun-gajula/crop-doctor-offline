package com.cropdoctor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.vision.ExecuTorchClassifier
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class V7SanityPhoneTest {

    private val TAG = "V7SanityPhoneTest"

    data class TestCase(
        val fileName: String,
        val trueLabel: String
    )

    @Test
    fun test5ValidationImagesOnPhone() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        KnowledgeBase.initialize(context)

        val classifier = ExecuTorchClassifier(context)

        val testCases = listOf(
            TestCase("/data/local/tmp/v7_sanity/sample1_rice.jpg", "rice_leaf_blast"),
            TestCase("/data/local/tmp/v7_sanity/sample2_cotton.jpg", "cotton_bacterial_blight"),
            TestCase("/data/local/tmp/v7_sanity/sample3_chilli.jpg", "chilli_leaf_curl_virus"),
            TestCase("/data/local/tmp/v7_sanity/sample4_tomato.jpg", "tomato_yellow_leaf_curl_virus"),
            TestCase("/data/local/tmp/v7_sanity/sample5_maize.jpg", "maize_northern_leaf_blight")
        )

        val resultsArray = JSONArray()

        for ((index, tc) in testCases.withIndex()) {
            val file = File(tc.fileName)
            assertTrue("Test image must exist: ${tc.fileName}", file.exists())

            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull("Failed to decode image: ${tc.fileName}", rawBitmap)

            // Ensure the input image to vision model is 512x512 pixels
            val bitmap512 = Bitmap.createScaledBitmap(rawBitmap, 512, 512, true)
            assertEquals512(bitmap512)

            Log.i(TAG, "[$index] Classifying ${tc.fileName} (True: ${tc.trueLabel})...")
            // Pass null for cropHint so all classes are considered (unmasked)
            val result = classifier.classify(bitmap512, null)

            val top1Label = if (result.top3.isNotEmpty()) result.top3[0].classId else result.classId
            val top1Prob = if (result.top3.isNotEmpty()) result.top3[0].probability else result.confidence

            Log.i(TAG, "[$index] Result: true=${tc.trueLabel}, top1=$top1Label, prob=$top1Prob, diag=${result.classId}, time=${result.inferenceTimeMs}ms")

            val obj = JSONObject().apply {
                put("index", index + 1)
                put("file_name", tc.fileName)
                put("true_label", tc.trueLabel)
                put("top1_label", top1Label)
                put("probability", top1Prob.toDouble())
                put("diagnosis_class_id", result.classId)
                put("diagnosis_display_name", result.displayName)
                put("confidence", result.confidence.toDouble())
                put("certainty_band", result.certaintyBand.name)
                put("severity_band", result.severityBand.name)
                put("inference_time_ms", result.inferenceTimeMs)
                put("memory_used_mb", result.memoryUsedMb)
            }
            resultsArray.put(obj)
        }

        // Save results to /data/local/tmp/v7_sanity/phone_results.json
        val outFile = File(context.filesDir, "phone_results.json")
        outFile.writeText(resultsArray.toString(2))
        Log.i(TAG, "Wrote phone results to ${outFile.absolutePath}")

        classifier.close()
    }

    private fun assertEquals512(bitmap: Bitmap) {
        if (bitmap.width != 512 || bitmap.height != 512) {
            throw AssertionError("Bitmap size must be 512x512, but was ${bitmap.width}x${bitmap.height}")
        }
    }
}
