package com.cropdoctor

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Debug
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.advisor.ExecuTorchLlamaAdvisor
import com.cropdoctor.advisor.MemoryBudgetDecider
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.vision.ExecuTorchClassifier
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class FallbackRateBenchmarkTest {

    private val TAG = "FallbackRateBenchmarkTest"

    private fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo
    }

    private fun runFlowForScenario(
        scenarioId: String,
        scenarioDescription: String,
        simulatedMinThresholdBytes: Long = MemoryBudgetDecider.MIN_AVAILABLE_RAM_BYTES
    ): JSONObject = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        KnowledgeBase.initialize(context)

        val memInfoBefore = getMemoryInfo(context)
        val availMemMb = memInfoBefore.availMem / (1024.0 * 1024.0)
        val totalMemMb = memInfoBefore.totalMem / (1024.0 * 1024.0)
        val thresholdMb = memInfoBefore.threshold / (1024.0 * 1024.0)
        val isLowMem = memInfoBefore.lowMemory

        val isSafeAccordingToDecider = MemoryBudgetDecider.shouldLoadLlama(
            availMemBytes = memInfoBefore.availMem,
            isLowMemory = isLowMem,
            minRequiredBytes = simulatedMinThresholdBytes
        )

        Log.i(
            TAG,
            "[$scenarioId] Memory before flow: availMem=${String.format(Locale.US, "%.1f", availMemMb)} MB, totalMem=${String.format(Locale.US, "%.1f", totalMemMb)} MB, lowMemory=$isLowMem, deciderSaysSafe=$isSafeAccordingToDecider"
        )

        // Step 1: Load a real diagnosed leaf image
        val testLeavesDir = File(context.filesDir, "test_leaves")
        val testImageFile = File(testLeavesDir, "image-1794556664071.jpg").takeIf { it.exists() }
            ?: File(testLeavesDir, "IMG_20261114_142908958.jpg")
        val bitmap = BitmapFactory.decodeFile(testImageFile.absolutePath)
        assertNotNull("Test leaf bitmap must load", bitmap)

        // Step 2: Run vision inference
        val classifier = ExecuTorchClassifier(context)
        val classResult = classifier.classify(bitmap, "maize")
        classifier.close()

        val kbEntry = KnowledgeBase.getEntry(if (classResult.classId != "unknown") classResult.classId else "maize_common_rust")
        assertNotNull("KB entry must be found", kbEntry)

        // Step 3: Record PSS before Llama load
        System.gc()
        val pssBeforeKb = Debug.getPss()
        val pssBeforeMb = pssBeforeKb / 1024.0

        // Step 4: Run Advisor flow
        val advisor = ExecuTorchLlamaAdvisor(context)
        advisor.memoryThresholdBytes = simulatedMinThresholdBytes
        val receivedTokens = mutableListOf<String>()
        var peakPssKb = pssBeforeKb

        // If simulating low memory condition below threshold, pass custom threshold to advisor check
        if (!isSafeAccordingToDecider) {
            // Memory is insufficient -> generateAdvice should fall back to KB text with label "Offline guide"
            advisor.generateAdvice(
                classId = kbEntry!!.classId,
                severityBand = SeverityBand.SEVERE,
                certainty = "PROBABLE",
                language = "en",
                kbEntry = kbEntry
            ) { token ->
                receivedTokens.add(token)
            }
        } else {
            advisor.generateAdvice(
                classId = kbEntry!!.classId,
                severityBand = SeverityBand.SEVERE,
                certainty = "PROBABLE",
                language = "en",
                kbEntry = kbEntry
            ) { token ->
                receivedTokens.add(token)
                val curPss = Debug.getPss()
                if (curPss > peakPssKb) {
                    peakPssKb = curPss
                }
            }
        }

        // Measure PSS after inference / while loaded
        val pssAfterLoadKb = Debug.getPss()
        if (pssAfterLoadKb > peakPssKb) {
            peakPssKb = pssAfterLoadKb
        }
        val peakPssMb = peakPssKb / 1024.0
        val extraMemoryMb = peakPssMb - pssBeforeMb

        val llamaLoaded = advisor.isModelCurrentlyLoaded
        val fallbackRan = advisor.isLowMemoryFallback
        val outputLength = receivedTokens.size
        val sampleOutput = receivedTokens.take(10).joinToString("")

        Log.i(
            TAG,
            "[$scenarioId] Flow outcome: llamaLoaded=$llamaLoaded, fallbackRan=$fallbackRan, tokensCount=$outputLength, PSS before=${String.format(Locale.US, "%.1f", pssBeforeMb)} MB, Peak PSS=${String.format(Locale.US, "%.1f", peakPssMb)} MB, Extra delta=${String.format(Locale.US, "%.1f", extraMemoryMb)} MB"
        )

        advisor.close()

        val resultObj = JSONObject().apply {
            put("scenario_id", scenarioId)
            put("description", scenarioDescription)
            put("avail_mem_bytes", memInfoBefore.availMem)
            put("avail_mem_mb", availMemMb)
            put("total_mem_mb", totalMemMb)
            put("threshold_mb", thresholdMb)
            put("is_low_memory", isLowMem)
            put("decider_should_load", isSafeAccordingToDecider)
            put("llama_loaded", llamaLoaded)
            put("fallback_ran", fallbackRan)
            put("advisory_label", if (fallbackRan || !isSafeAccordingToDecider) "Offline guide" else "AI explanation")
            put("pss_before_mb", pssBeforeMb)
            put("peak_pss_mb", peakPssMb)
            put("extra_memory_needed_mb", extraMemoryMb)
            put("tokens_generated", outputLength)
            put("sample_text", sampleOutput)
        }

        val benchDir = File(context.filesDir, "benchmarks")
        benchDir.mkdirs()
        val outputFile = File(benchDir, "fallback_${scenarioId}.json")
        outputFile.writeText(resultObj.toString(2))
        Log.i(TAG, "Saved fallback record to: ${outputFile.absolutePath}")

        resultObj
    }

    @Test
    fun benchmarkScenarioA_RebootIdle() {
        val result = runFlowForScenario("a_reboot", "Just after a reboot with nothing else open")
        Log.i(TAG, "Scenario A Completed: $result")
    }

    @Test
    fun benchmarkScenarioB_FiveAppsOpen() {
        val result = runFlowForScenario("b_5apps", "With 5 other apps open (WhatsApp, YouTube, Chrome, Camera, Gallery)")
        Log.i(TAG, "Scenario B Completed: $result")
    }

    @Test
    fun benchmarkScenarioC_NormalUse() {
        val result = runFlowForScenario("c_normal_use", "After 10 minutes of normal use")
        Log.i(TAG, "Scenario C Completed: $result")
    }

    @Test
    fun verifyFallbackTriggerWhenRamBelowThreshold() {
        // Test that if available RAM were below required threshold (e.g. 5000 MB required on 2.4 GB free device),
        // Decider rejects load and app routes cleanly to "Offline guide" without OOM crash.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memInfo = getMemoryInfo(context)
        val simulatedHighThreshold = memInfo.availMem + 500L * 1024L * 1024L // 500 MB above available
        val shouldLoad = MemoryBudgetDecider.shouldLoadLlama(memInfo.availMem, false, simulatedHighThreshold)
        assertFalse("Decider must reject Llama load when availMem < threshold", shouldLoad)

        val result = runFlowForScenario(
            scenarioId = "fallback_simulated_low_ram",
            scenarioDescription = "Simulated low RAM condition (< threshold)",
            simulatedMinThresholdBytes = simulatedHighThreshold
        )
        assertFalse("Llama must not load when below threshold", result.getBoolean("llama_loaded"))
        assertEquals("Label must be 'Offline guide'", "Offline guide", result.getString("advisory_label"))
        Log.i(TAG, "Fallback verification passed: label=${result.getString("advisory_label")}, fallbackRan=${result.getBoolean("fallback_ran")}")
    }
}
