package com.cropdoctor

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule
import java.io.File

@RunWith(AndroidJUnit4::class)
class LlamaPhoneSanityTest {

    private val TAG = "LlamaPhoneSanityTest"

    @Test
    fun testLlamaModuleLoadAndGenerateOnPhone() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        var modelFile = File(context.filesDir, "llama-3.2-1b-spinquant.pte")
        var tokenizerFile = File(context.filesDir, "tokenizer.model")

        if (!modelFile.exists() || modelFile.length() < 100_000_000L) {
            modelFile = File("/data/local/tmp/llama-3.2-1b-spinquant.pte")
        }
        if (!tokenizerFile.exists() || tokenizerFile.length() < 100_000L) {
            tokenizerFile = File("/data/local/tmp/tokenizer.model")
        }

        Log.i(TAG, "Testing Llama with model: ${modelFile.absolutePath} (${modelFile.length()} bytes)")
        Log.i(TAG, "Testing Llama with tokenizer: ${tokenizerFile.absolutePath} (${tokenizerFile.length()} bytes)")

        assertTrue("Model file must exist and be > 100MB", modelFile.exists() && modelFile.length() > 100_000_000L)
        assertTrue("Tokenizer file must exist", tokenizerFile.exists() && tokenizerFile.length() > 100_000L)

        val loadStart = SystemClock.elapsedRealtime()
        val llama = LlmModule(modelFile.absolutePath, tokenizerFile.absolutePath, 0.7f)
        val loadStatus = llama.load()
        val loadDuration = SystemClock.elapsedRealtime() - loadStart

        Log.i(TAG, "LlmModule.load() status: $loadStatus in $loadDuration ms")
        assertEquals("Llama load status must be 0 (SUCCESS)", 0, loadStatus)

        val generatedTokens = StringBuilder()
        var tokenCount = 0
        val genStart = SystemClock.elapsedRealtime()

        val callback = object : LlmCallback {
            override fun onResult(token: String?) {
                if (!token.isNullOrEmpty()) {
                    tokenCount++
                    generatedTokens.append(token)
                    Log.i(TAG, "Token [$tokenCount]: $token")
                }
            }

            override fun onStats(stats: Float) {
                Log.i(TAG, "onStats: $stats")
            }
        }

        val prompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\nYou are an agricultural advisor.<|eot_id|><|start_header_id|>user<|end_header_id|>\nSay one word: Healthy<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n"
        val genStatus = llama.generate(prompt, 10, callback)
        val genDuration = SystemClock.elapsedRealtime() - genStart
        val tokPerSec = if (genDuration > 0) (tokenCount * 1000.0) / genDuration else 0.0

        Log.i(TAG, "Generated $tokenCount tokens in $genDuration ms ($tokPerSec tokens/s). Status: $genStatus")
        Log.i(TAG, "Full output: $generatedTokens")

        llama.resetNative()
        assertTrue("Must generate at least 1 token", tokenCount > 0)
    }
}
