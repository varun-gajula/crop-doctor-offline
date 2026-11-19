package com.cropdoctor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.vision.ExecuTorchClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShutterToResultReleaseBenchmarkTest {

    private val TAG = "ShutterBenchmark"

    @Test
    fun benchmarkShutterToResultFullPipeline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        KnowledgeBase.initialize(context)

        val classifier = ExecuTorchClassifier(context)

        // Warm-up run
        val warmupBmp = createSyntheticLeafBitmap(512, 512, 0)
        classifier.classify(warmupBmp, null)

        val times = mutableListOf<Long>()
        val results = mutableListOf<String>()

        for (i in 1..10) {
            val bmp = createSyntheticLeafBitmap(1024, 768, i) // camera full size capture
            val tStart = SystemClock.elapsedRealtime()

            val result = classifier.classify(bmp, null)

            val tTotal = SystemClock.elapsedRealtime() - tStart
            times.add(tTotal)
            results.add("Run $i: ${tTotal}ms | Class: ${result.classId} (${result.displayName}) | Conf: ${"%.3f".format(result.confidence)} | Certainty: ${result.certaintyBand} | Severity: ${result.severityBand}")
            Log.i(TAG, "Run $i: ${tTotal}ms (Class=${result.classId}, Conf=${result.confidence})")
        }

        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0L
        val maxTime = times.maxOrNull() ?: 0L

        val logSummary = buildString {
            appendLine("=== SHUTTER-TO-RESULT BENCHMARK (10 RUNS) ===")
            appendLine("Target Budget: <= 1,500 ms (1.5 s)")
            appendLine("Average Shutter-to-Result: ${"%.2f".format(avgTime)} ms")
            appendLine("Min: ${minTime} ms, Max: ${maxTime} ms")
            appendLine("Status: ${if (avgTime <= 1500) "PASS" else "FAIL"}")
            appendLine("--- Detailed Runs ---")
            results.forEach { appendLine(it) }
            appendLine("=============================================")
        }

        Log.i(TAG, logSummary)
        println(logSummary)

        assertTrue("Average shutter-to-result ($avgTime ms) exceeds 1500ms budget", avgTime <= 1500)
        classifier.close()
    }

    private fun createSyntheticLeafBitmap(width: Int, height: Int, seed: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color = Color.rgb(46, 125, 50) // Leaf green
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw lesions (brown/yellow necrotic spots)
        val lesionPaint = Paint().apply {
            color = Color.rgb(139, 69, 19)
            isAntiAlias = true
        }
        val spotSize = width / 10f
        for (j in 1..4) {
            val cx = (width * (j * 2) / 10f) + (seed * 5f) % 40f
            val cy = (height * (j * 2) / 10f) + (seed * 7f) % 40f
            canvas.drawCircle(cx, cy, spotSize, lesionPaint)
        }
        return bmp
    }
}
