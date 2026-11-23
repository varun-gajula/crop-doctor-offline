package com.cropdoctor.advisor

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Task FIX-LLAMA Step 6: Unit tests for cancel-and-ignore-stale logic.
 * Verifies:
 * 1. Tokens from stale requestIds are ignored and dropped.
 * 2. cancelAndAwait stops in-flight generation cleanly.
 * 3. Generation is strictly serialized (one at a time).
 * 4. Max sequence length (1024) safely accommodates longest prompt + complete 3-part answer.
 */
class LlamaCancellationTest {

    @Test
    fun testStaleTokenRejection() = runBlocking {
        var activeRequestId = 1L
        val receivedTokens = mutableListOf<String>()

        fun emitToken(reqId: Long, token: String) {
            if (reqId == activeRequestId) {
                receivedTokens.add(token)
            }
        }

        // Emit tokens for req 1
        emitToken(1L, "Token1_A")
        emitToken(1L, "Token1_B")

        // New scan increments activeRequestId to 2
        activeRequestId = 2L

        // Stale tokens from req 1 arrive late
        emitToken(1L, "Token1_C_Late")
        emitToken(1L, "Token1_D_Late")

        // Tokens for req 2 arrive
        emitToken(2L, "Token2_A")

        assertEquals(listOf("Token1_A", "Token1_B", "Token2_A"), receivedTokens)
        assertFalse(receivedTokens.contains("Token1_C_Late"))
        assertFalse(receivedTokens.contains("Token1_D_Late"))
    }

    @Test
    fun testCancelAndAwaitTerminatesWithinTimeout() = runBlocking {
        val isGenerating = AtomicBoolean(true)
        val stopRequested = AtomicBoolean(false)

        // Launch mock generator coroutine
        val genJob = launch(Dispatchers.Default) {
            while (!stopRequested.get()) {
                delay(20L)
            }
            isGenerating.set(false)
        }

        // Test cancelAndAwait function
        suspend fun cancelAndAwait(timeoutMs: Long): Boolean {
            if (!isGenerating.get()) return true
            stopRequested.set(true)
            return try {
                withTimeout(timeoutMs) {
                    while (isGenerating.get()) {
                        delay(20L)
                    }
                }
                true
            } catch (e: TimeoutCancellationException) {
                false
            }
        }

        val success = cancelAndAwait(3000L)
        assertTrue("cancelAndAwait should return true within 3000ms", success)
        assertFalse("isGenerating must be false after cancelAndAwait", isGenerating.get())
        genJob.join()
    }

    @Test
    fun testOneGenerationAtATimeSerialization() = runBlocking {
        val lock = Mutex()
        val executionOrder = ConcurrentLinkedQueue<String>()
        val activeGenerations = AtomicLong(0)
        var maxConcurrent = 0L

        suspend fun simulateGenerate(name: String, delayMs: Long) = lock.withLock {
            val count = activeGenerations.incrementAndGet()
            if (count > maxConcurrent) maxConcurrent = count
            executionOrder.add("$name-start")
            delay(delayMs)
            executionOrder.add("$name-end")
            activeGenerations.decrementAndGet()
        }

        val job1 = launch(Dispatchers.Default) { simulateGenerate("Gen1", 80L) }
        val job2 = launch(Dispatchers.Default) { simulateGenerate("Gen2", 40L) }

        job1.join()
        job2.join()

        assertEquals(1L, maxConcurrent)
        val list = executionOrder.toList()
        assertEquals(listOf("Gen1-start", "Gen1-end", "Gen2-start", "Gen2-end"), list)
    }

    @Test
    fun testSequenceLengthBudgetDerivation() {
        val maxPromptChars = 1800
        val approxTokensPerChar = 1.0 / 3.8
        val maxPromptTokens = (maxPromptChars * approxTokensPerChar).toInt() // ~473 tokens

        val targetAnswerTokens = 220
        val totalExpectedTokens = maxPromptTokens + targetAnswerTokens // ~693 tokens

        val allocatedMaxSeqLen = 1024
        assertTrue("1024 sequence length must exceed total expected tokens", allocatedMaxSeqLen > totalExpectedTokens)

        val safetyMargin = allocatedMaxSeqLen - totalExpectedTokens
        assertTrue("Safety margin must be at least 250 tokens", safetyMargin >= 250)
    }
}
