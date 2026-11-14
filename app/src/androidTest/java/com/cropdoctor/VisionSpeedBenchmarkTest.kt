package com.cropdoctor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.vision.ExecuTorchClassifier
import com.cropdoctor.vision.LeafValidator
import com.cropdoctor.vision.LesionAreaEstimator
import com.cropdoctor.vision.VisionMath
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class VisionSpeedBenchmarkTest {

    private val TAG = "VisionSpeedBenchmarkTest"

    @Test
    fun benchmarkRealLeavesShutterToResult() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        KnowledgeBase.initialize(context)

        val classifier = ExecuTorchClassifier(context)

        // Warm up run
        val warmupBmp = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        classifier.classify(warmupBmp, null)

        val testLeavesDir = File(context.filesDir, "test_leaves")
        assertTrue("Test leaves dir must exist at ${testLeavesDir.absolutePath}", testLeavesDir.exists())

        val imageFiles = testLeavesDir.listFiles { file ->
            val name = file.name.lowercase(Locale.ROOT)
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
        }?.sortedBy { it.name } ?: emptyList()

        Log.i(TAG, "Found ${imageFiles.size} candidate leaf images in ${testLeavesDir.absolutePath}")
        assertTrue("Must have at least 10 candidate images, found: ${imageFiles.size}", imageFiles.size >= 10)

        val labelsStream = context.assets.open("labels.json")
        val labelsJson = labelsStream.bufferedReader().use { it.readText() }
        val labelsArray = JSONArray(labelsJson)
        val labels = (0 until labelsArray.length()).map { labelsArray.getString(it) }

        val defaultThresh = classifier.defaultThreshold ?: 0.40f
        val energyThresh = classifier.energyThreshold ?: -1.5
        val classThresholds = classifier.classThresholds
        val unsupportedClasses = classifier.unsupportedClasses

        val modelFile = File(context.filesDir, "vision.pte")
        val module = Module.load(modelFile.absolutePath, Module.LOAD_MODE_FILE)

        val diagnosedRuns = mutableListOf<JSONObject>()
        val rejectedRuns = mutableListOf<JSONObject>()

        for (file in imageFiles) {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            if (originalBitmap == null) {
                Log.w(TAG, "Could not decode file: ${file.name}")
                rejectedRuns.add(JSONObject().apply {
                    put("file_name", file.name)
                    put("reason", "Corrupt or unreadable image file")
                })
                continue
            }

            val tStart = SystemClock.elapsedRealtimeNanos()

            // 1. Validation (LeafValidator)
            val tValStart = SystemClock.elapsedRealtimeNanos()
            val valResult = LeafValidator.validate(originalBitmap)
            val valDurationNanos = SystemClock.elapsedRealtimeNanos() - tValStart

            // 2. Preprocessing (Resize 320x320 + Normalization)
            val tPrepStart = SystemClock.elapsedRealtimeNanos()
            val resized = Bitmap.createScaledBitmap(originalBitmap, 320, 320, true)
            val inputTensor = classifier.preprocessBitmap(resized)
            val prepDurationNanos = SystemClock.elapsedRealtimeNanos() - tPrepStart

            // 3. Inference (ExecuTorch vision forward pass on Arm CPU)
            val tInfStart = SystemClock.elapsedRealtimeNanos()
            val outputs = module.forward(EValue.from(inputTensor))
            val outputTensor = outputs[0].toTensor()
            val rawLogits = outputTensor.dataAsFloatArray
            val infDurationNanos = SystemClock.elapsedRealtimeNanos() - tInfStart

            // 4. Calibration & Post-processing (Temperature scaling, Free energy, Softmax, OOD check)
            val tCalStart = SystemClock.elapsedRealtimeNanos()
            val scaledLogits = VisionMath.applyTemperature(rawLogits, 1.0f)
            val freeEnergy = VisionMath.computeFreeEnergy(scaledLogits, 1.0f)
            val probabilities = VisionMath.maskAndRenormalize(labels, scaledLogits, null)
            val eval = VisionMath.evaluateDiagnosis(
                labels = labels,
                probabilities = probabilities,
                thresholds = classThresholds,
                defaultThreshold = defaultThresh,
                freeEnergy = freeEnergy,
                energyThreshold = energyThresh,
                unsupportedClasses = unsupportedClasses
            )
            val calDurationNanos = SystemClock.elapsedRealtimeNanos() - tCalStart

            // Check if diagnosed or rejected
            val isDiagnosed = eval.classId != "unknown"

            if (!isDiagnosed) {
                val reason = when {
                    eval.isOutOfDistribution -> "Rejected: Out of distribution (Free energy = ${String.format(Locale.US, "%.2f", freeEnergy)} < threshold $energyThresh)"
                    eval.isBelowThreshold -> "Rejected: Below confidence threshold (Confidence = ${String.format(Locale.US, "%.3f", eval.confidence)} < threshold ${classThresholds[eval.top3.firstOrNull()?.classId] ?: defaultThresh})"
                    unsupportedClasses.contains(eval.top3.firstOrNull()?.classId) -> "Rejected: Unsupported class (${eval.top3.firstOrNull()?.classId})"
                    else -> "Rejected: Unknown classification"
                }
                Log.w(TAG, "Image ${file.name} was REJECTED: $reason")
                rejectedRuns.add(JSONObject().apply {
                    put("file_name", file.name)
                    put("reason", reason)
                    put("free_energy", freeEnergy)
                    put("top_class", eval.top3.firstOrNull()?.classId ?: "none")
                    put("top_prob", eval.top3.firstOrNull()?.probability ?: 0f)
                })
                continue
            }

            // 5. Lesion fraction estimation
            val tLesionStart = SystemClock.elapsedRealtimeNanos()
            val lesionFraction = if (eval.classId.endsWith("_healthy")) {
                0.0f
            } else {
                LesionAreaEstimator.estimateLesionFraction(originalBitmap)
            }
            val lesionDurationNanos = SystemClock.elapsedRealtimeNanos() - tLesionStart

            // 6. Rules engine (Severity and Tier routing)
            val tRulesStart = SystemClock.elapsedRealtimeNanos()
            val severityBand = if (eval.classId.endsWith("_healthy")) {
                SeverityBand.MILD
            } else {
                RulesEngine.calculateSeverity(lesionFraction, eval.classId)
            }
            val openTier = RulesEngine.determineOpenTier(severityBand)
            val rulesDurationNanos = SystemClock.elapsedRealtimeNanos() - tRulesStart

            // 7. Grounded Knowledge Base lookup
            val tKbStart = SystemClock.elapsedRealtimeNanos()
            val kbEntry = KnowledgeBase.getEntry(eval.classId)
            val kbDurationNanos = SystemClock.elapsedRealtimeNanos() - tKbStart

            val tTotalNanos = SystemClock.elapsedRealtimeNanos() - tStart

            val valMs = valDurationNanos / 1_000_000.0
            val prepMs = prepDurationNanos / 1_000_000.0
            val infMs = infDurationNanos / 1_000_000.0
            val calMs = calDurationNanos / 1_000_000.0
            val lesionMs = lesionDurationNanos / 1_000_000.0
            val rulesMs = rulesDurationNanos / 1_000_000.0
            val kbMs = kbDurationNanos / 1_000_000.0
            val totalMs = tTotalNanos / 1_000_000.0

            val runRecord = JSONObject().apply {
                put("file_name", file.name)
                put("class_id", eval.classId)
                put("display_name", eval.displayName)
                put("confidence", eval.confidence)
                put("certainty_band", eval.certaintyBand.name)
                put("free_energy", freeEnergy)
                put("lesion_fraction", lesionFraction)
                put("severity_band", severityBand.name)
                put("open_tier", openTier.name)
                put("kb_resolved", kbEntry != null)
                put("validation_ms", valMs)
                put("preprocessing_ms", prepMs)
                put("inference_ms", infMs)
                put("calibration_ms", calMs)
                put("lesion_estimate_ms", lesionMs)
                put("rules_engine_ms", rulesMs)
                put("kb_lookup_ms", kbMs)
                put("shutter_to_result_ms", totalMs)
            }

            diagnosedRuns.add(runRecord)
            Log.i(
                TAG,
                "DIAGNOSED #${diagnosedRuns.size} [${file.name}]: ${eval.classId} (${eval.displayName}) " +
                        "conf=${String.format(Locale.US, "%.3f", eval.confidence)}, " +
                        "total=${String.format(Locale.US, "%.1f", totalMs)} ms (inf=${String.format(Locale.US, "%.1f", infMs)} ms, lesion=${String.format(Locale.US, "%.1f", lesionMs)} ms)"
            )

            if (diagnosedRuns.size == 10) {
                Log.i(TAG, "Reached 10 diagnosed runs from real field photos.")
                break
            }
        }

        classifier.close()

        Log.i(TAG, "Benchmark completed. Diagnosed: ${diagnosedRuns.size}, Rejected: ${rejectedRuns.size}")
        assertTrue("Must have at least 10 diagnosed runs, found: ${diagnosedRuns.size}", diagnosedRuns.size >= 10)

        val totalTimes = (0 until 10).map { diagnosedRuns[it].getDouble("shutter_to_result_ms") }
        val infTimes = (0 until 10).map { diagnosedRuns[it].getDouble("inference_ms") }
        val prepTimes = (0 until 10).map { diagnosedRuns[it].getDouble("preprocessing_ms") }
        val valTimes = (0 until 10).map { diagnosedRuns[it].getDouble("validation_ms") }
        val calTimes = (0 until 10).map { diagnosedRuns[it].getDouble("calibration_ms") }
        val lesionTimes = (0 until 10).map { diagnosedRuns[it].getDouble("lesion_estimate_ms") }
        val rulesTimes = (0 until 10).map { diagnosedRuns[it].getDouble("rules_engine_ms") }
        val kbTimes = (0 until 10).map { diagnosedRuns[it].getDouble("kb_lookup_ms") }

        val avgTotal = totalTimes.average()
        val avgInf = infTimes.average()
        val avgPrep = prepTimes.average()
        val avgVal = valTimes.average()
        val avgCal = calTimes.average()
        val avgLesion = lesionTimes.average()
        val avgRules = rulesTimes.average()
        val avgKb = kbTimes.average()

        Log.i(TAG, "=== 10-RUN DIAGNOSED BENCHMARK AVERAGES ===")
        Log.i(TAG, "Validation:          ${String.format(Locale.US, "%.2f", avgVal)} ms")
        Log.i(TAG, "Preprocessing:       ${String.format(Locale.US, "%.2f", avgPrep)} ms")
        Log.i(TAG, "Vision Inference:    ${String.format(Locale.US, "%.2f", avgInf)} ms")
        Log.i(TAG, "Calibration (OOD):   ${String.format(Locale.US, "%.2f", avgCal)} ms")
        Log.i(TAG, "Lesion Estimate:     ${String.format(Locale.US, "%.2f", avgLesion)} ms")
        Log.i(TAG, "Rules Engine:        ${String.format(Locale.US, "%.2f", avgRules)} ms")
        Log.i(TAG, "KB Retrieval:        ${String.format(Locale.US, "%.2f", avgKb)} ms")
        Log.i(TAG, "FULL SHUTTER-TO-RESULT: ${String.format(Locale.US, "%.2f", avgTotal)} ms (Target <= 1500 ms)")

        val summaryObj = JSONObject().apply {
            put("diagnosed_runs_count", 10)
            put("rejected_runs_count", rejectedRuns.size)
            put("average_shutter_to_result_ms", avgTotal)
            put("min_shutter_to_result_ms", totalTimes.minOrNull() ?: 0.0)
            put("max_shutter_to_result_ms", totalTimes.maxOrNull() ?: 0.0)
            put("average_breakdown", JSONObject().apply {
                put("validation_ms", avgVal)
                put("preprocessing_ms", avgPrep)
                put("inference_ms", avgInf)
                put("calibration_ms", avgCal)
                put("lesion_estimate_ms", avgLesion)
                put("rules_engine_ms", avgRules)
                put("kb_lookup_ms", avgKb)
            })
            put("diagnosed_runs", JSONArray(diagnosedRuns.take(10)))
            put("rejected_runs", JSONArray(rejectedRuns))
        }

        val benchDir = File(context.filesDir, "benchmarks")
        benchDir.mkdirs()
        val outputFile = File(benchDir, "speed_summary_real_leaves.json")
        outputFile.writeText(summaryObj.toString(2))
        Log.i(TAG, "Saved real leaves speed summary to: ${outputFile.absolutePath}")

        assertTrue("Average shutter-to-result must be <= 1500 ms (1.5s), was: $avgTotal ms", avgTotal <= 1500.0)
    }
}
