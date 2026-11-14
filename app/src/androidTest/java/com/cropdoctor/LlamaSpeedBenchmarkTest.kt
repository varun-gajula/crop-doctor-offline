package com.cropdoctor

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.advisor.GuardrailFilter
import com.cropdoctor.advisor.PromptBuilder
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KnowledgeBase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LlamaSpeedBenchmarkTest {

    private val TAG = "LlamaSpeedBenchmarkTest"

    data class TestCase(
        val classId: String,
        val crop: String,
        val diseaseName: String,
        val severity: SeverityBand,
        val certainty: String,
        val language: String
    )

    @Test
    fun benchmarkLlamaSpeed5Cases() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        KnowledgeBase.initialize(context)

        var modelFile = File(context.filesDir, "llama-3.2-1b-spinquant.pte")
        var tokenizerFile = File(context.filesDir, "tokenizer.model")

        if (!modelFile.exists() || modelFile.length() < 100_000_000L) {
            modelFile = File("/data/local/tmp/llama-3.2-1b-spinquant.pte")
        }
        if (!tokenizerFile.exists() || tokenizerFile.length() < 100_000L) {
            tokenizerFile = File("/data/local/tmp/tokenizer.model")
        }

        assertTrue("Model file must exist and be > 100MB", modelFile.exists() && modelFile.length() > 100_000_000L)
        assertTrue("Tokenizer file must exist", tokenizerFile.exists() && tokenizerFile.length() > 100_000L)

        Log.i(TAG, "Loading LlmModule from: ${modelFile.absolutePath}...")
        val loadStart = SystemClock.elapsedRealtime()
        val llama = LlmModule(modelFile.absolutePath, tokenizerFile.absolutePath, 0.7f)
        val loadStatus = llama.load()
        val loadDuration = SystemClock.elapsedRealtime() - loadStart

        Log.i(TAG, "LlmModule.load() status: $loadStatus in $loadDuration ms")
        assertEquals("Llama load status must be 0 (SUCCESS)", 0, loadStatus)

        val testCases = listOf(
            TestCase("maize_common_rust", "maize", "Common Rust", SeverityBand.MODERATE, "Probably", "en"),
            TestCase("tomato_early_blight", "tomato", "Early Blight", SeverityBand.MILD, "Sure", "en"),
            TestCase("rice_leaf_blast", "rice", "Leaf Blast", SeverityBand.SEVERE, "Sure", "en"),
            TestCase("cotton_bacterial_blight", "cotton", "Bacterial Blight", SeverityBand.MODERATE, "Probably", "en"),
            TestCase("chilli_leaf_curl_virus", "chilli", "Leaf Curl", SeverityBand.SEVERE, "Sure", "en")
        )

        val resultsArray = JSONArray()

        for ((index, tc) in testCases.withIndex()) {
            val kbEntry = KnowledgeBase.getEntry(tc.classId)
                ?: error("KB entry for ${tc.classId} not found")

            val urgentStep = kbEntry.immediateStep.en.ifEmpty { "Remove infected leaves." }
            val naturalRemedy = if (kbEntry.treatment.natural.isNotEmpty()) {
                kbEntry.treatment.natural.first().action.en
            } else "Field hygiene"
            val chemicalActives = if (kbEntry.treatment.chemical.isNotEmpty()) {
                kbEntry.treatment.chemical.first().activeIngredient
            } else "Follow label"

            val prompt = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
You are Crop Doctor, an offline farm assistant. Explain concisely based only on official advice.<|eot_id|><|start_header_id|>user<|end_header_id|>
Crop: ${tc.crop}
Diagnosis: ${tc.diseaseName}
Severity: ${tc.severity.name}
Verified treatment: $urgentStep. Natural: $naturalRemedy. Chemical: $chemicalActives.
Explain what this is and practical steps for the farmer.<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""

            val generatedTokens = StringBuilder()
            var tokenCount = 0
            var firstTokenTimeMs = 0L
            val genStart = SystemClock.elapsedRealtime()

            val callback = object : LlmCallback {
                override fun onResult(token: String?) {
                    if (!token.isNullOrEmpty()) {
                        val now = SystemClock.elapsedRealtime()
                        if (firstTokenTimeMs == 0L) {
                            firstTokenTimeMs = now - genStart
                        }
                        tokenCount++
                        generatedTokens.append(token)
                    }
                }

                override fun onStats(stats: Float) {
                    // Optional stats
                }
            }

            Log.i(TAG, "--- Running Case ${index + 1}/5: ${tc.crop} - ${tc.diseaseName} ---")
            // Generate concise advice with echo = false for accurate TTFT and token count
            val genStatus = llama.generate(prompt, 256, callback, false)
            val genTotalDuration = SystemClock.elapsedRealtime() - genStart
            val decodeDuration = genTotalDuration - firstTokenTimeMs
            val tokensPerSec = if (decodeDuration > 0) (tokenCount.toDouble() * 1000.0) / decodeDuration else 0.0
            val overallTokensPerSec = if (genTotalDuration > 0) (tokenCount.toDouble() * 1000.0) / genTotalDuration else 0.0

            val fullText = generatedTokens.toString().trim()
            val guardrail = GuardrailFilter.validateAdvice(fullText, kbEntry, tc.severity)

            Log.i(TAG, "Case ${index + 1} Result: TTFT=${firstTokenTimeMs}ms, TotalTime=${genTotalDuration}ms, Tokens=$tokenCount, Speed=${String.format(Locale.US, "%.2f", tokensPerSec)} tok/s (overall ${String.format(Locale.US, "%.2f", overallTokensPerSec)} tok/s), Guardrail=${guardrail.isValid}")
            Log.i(TAG, "Output preview: ${fullText.take(150)}...")

            val caseObj = JSONObject().apply {
                put("case_index", index + 1)
                put("crop", tc.crop)
                put("disease_name", tc.diseaseName)
                put("class_id", tc.classId)
                put("severity", tc.severity.name)
                put("certainty", tc.certainty)
                put("prompt_length_chars", prompt.length)
                put("first_token_time_ms", firstTokenTimeMs)
                put("total_generation_time_ms", genTotalDuration)
                put("tokens_generated", tokenCount)
                put("tokens_per_second", tokensPerSec)
                put("overall_tokens_per_second", overallTokensPerSec)
                put("guardrail_valid", guardrail.isValid)
                put("guardrail_reason", guardrail.failureReason ?: "PASS")
                put("generated_text", fullText)
            }
            resultsArray.put(caseObj)

            // Save after each case for persistence
            val benchDir = File(context.filesDir, "benchmarks")
            benchDir.mkdirs()
            File(benchDir, "llama_5cases_speed.json").writeText(resultsArray.toString(2))

            assertTrue("Each case must generate tokens", tokenCount > 0)
        }

        llama.resetNative()

        val benchDir = File(context.filesDir, "benchmarks")
        benchDir.mkdirs()
        val outputFile = File(benchDir, "llama_5cases_speed.json")
        outputFile.writeText(resultsArray.toString(2))
        Log.i(TAG, "All 5 cases completed successfully! Saved to: ${outputFile.absolutePath}")
    }
}
