package com.cropdoctor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.vision.ExecuTorchClassifier
import com.cropdoctor.vision.LeafValidator
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class VisionSpeedBenchmarkTest {

    private val TAG = "VisionSpeedBenchmarkTest"

    @Test
    fun benchmarkShutterToResult10Runs() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        KnowledgeBase.initialize(context)

        val classifier = ExecuTorchClassifier(context)

        // Warm up run (discarded)
        val warmupBmp = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        warmupBmp.eraseColor(Color.rgb(34, 139, 34))
        classifier.classify(warmupBmp, "tomato")

        val runLatencies = mutableListOf<Long>()
        val resultsArray = JSONArray()

        Log.i(TAG, "Starting 10-run Shutter-to-Result Benchmark on Arm SoC...")

        for (i in 1..10) {
            val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
            // Vary slightly to prevent static caching
            val greenVal = 130 + (i % 10)
            bitmap.eraseColor(Color.rgb(34, greenVal, 34))

            val tStart = SystemClock.elapsedRealtime()

            // Step 1: Leaf validation (Rule H11)
            val validation = LeafValidator.validate(bitmap)

            // Step 2: ExecuTorch vision inference + energy + calibration
            val classResult = classifier.classify(bitmap, "tomato")

            // Step 3: Rules engine severity & certainty routing
            val severity = RulesEngine.calculateSeverity(classResult.lesionFraction, classResult.classId)
            val openTier = RulesEngine.determineOpenTier(severity)

            // Step 4: Grounded KnowledgeBase retrieval
            val kbEntry = KnowledgeBase.getEntry(classResult.classId)

            val tEnd = SystemClock.elapsedRealtime()
            val totalLatencyMs = tEnd - tStart
            runLatencies.add(totalLatencyMs)

            Log.i(TAG, "Run #$i: Shutter-to-Result = ${totalLatencyMs} ms (Inference = ${classResult.inferenceTimeMs} ms, Disease = ${classResult.displayName})")

            val runObj = JSONObject().apply {
                put("run_index", i)
                put("shutter_to_result_ms", totalLatencyMs)
                put("vision_inference_ms", classResult.inferenceTimeMs)
                put("class_id", classResult.classId)
                put("display_name", classResult.displayName)
                put("target_ms", 1500)
                put("pass", totalLatencyMs <= 1500)
            }
            resultsArray.put(runObj)
        }

        classifier.close()

        val avgLatency = runLatencies.average()
        val minLatency = runLatencies.minOrNull() ?: 0
        val maxLatency = runLatencies.maxOrNull() ?: 0

        Log.i(TAG, "Shutter-to-Result Summary (10 runs): Avg = ${String.format(Locale.US, "%.1f", avgLatency)} ms, Min = ${minLatency} ms, Max = ${maxLatency} ms")

        val summaryObj = JSONObject().apply {
            put("runs_count", 10)
            put("average_shutter_to_result_ms", avgLatency)
            put("min_ms", minLatency)
            put("max_ms", maxLatency)
            put("target_max_ms", 1500)
            put("pass_all", runLatencies.all { it <= 1500 })
            put("runs", resultsArray)
        }

        val benchDir = File(context.filesDir, "benchmarks")
        benchDir.mkdirs()
        val outputFile = File(benchDir, "shutter_to_result_10runs.json")
        outputFile.writeText(summaryObj.toString(2))
        Log.i(TAG, "Saved benchmark log to: ${outputFile.absolutePath}")

        assertTrue("Average shutter-to-result must be <= 1500 ms (1.5s)", avgLatency <= 1500.0)
    }
}
