package com.cropdoctor.advisor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MemoryBudgetDecider.shouldLoadLlama].
 *
 * Verifies Task M1 & Rule H15 requirements:
 * If available RAM is too low (< 1500 MB) or isLowMemory flag is set,
 * skip Llama and fall back to KB text with label "Offline guide".
 */
class MemoryBudgetDeciderTest {

    private val mbToBytes: (Long) -> Long = { mb -> mb * 1024L * 1024L }

    @Test
    fun shouldLoadLlama_returnsTrue_whenRamAbove1500MbAndNotLowMemory() {
        val availMem = mbToBytes(2048) // 2 GB free
        val isLowMemory = false

        val decision = MemoryBudgetDecider.shouldLoadLlama(availMem, isLowMemory)
        assertTrue("Should allow loading Llama when 2 GB RAM is available", decision)
    }

    @Test
    fun shouldLoadLlama_returnsFalse_whenSystemFlagsLowMemoryEvenWithMemoryAvailable() {
        val availMem = mbToBytes(2048) // 2 GB reported, but system flags lowMemory
        val isLowMemory = true

        val decision = MemoryBudgetDecider.shouldLoadLlama(availMem, isLowMemory)
        assertFalse("Must skip Llama when system flags lowMemory is true", decision)
    }

    @Test
    fun shouldLoadLlama_returnsFalse_whenAvailableRamBelowThreshold() {
        val availMem = mbToBytes(1200) // 1.2 GB free (< 1500 MB)
        val isLowMemory = false

        val decision = MemoryBudgetDecider.shouldLoadLlama(availMem, isLowMemory)
        assertFalse("Must skip Llama when available RAM (1200 MB) < 1500 MB threshold", decision)
    }

    @Test
    fun shouldLoadLlama_exactBoundaryCases() {
        val exact1500Mb = mbToBytes(1500)
        assertTrue(
            "Exactly 1500 MB available should allow Llama",
            MemoryBudgetDecider.shouldLoadLlama(exact1500Mb, false)
        )

        val oneByteBelow = exact1500Mb - 1L
        assertFalse(
            "1 byte below 1500 MB should reject Llama loading",
            MemoryBudgetDecider.shouldLoadLlama(oneByteBelow, false)
        )
    }

    @Test
    fun shouldLoadLlama_customThreshold() {
        val customThreshold = mbToBytes(1000)
        val availMem = mbToBytes(1200)

        assertTrue(
            "1200 MB should be sufficient when custom threshold is 1000 MB",
            MemoryBudgetDecider.shouldLoadLlama(availMem, false, customThreshold)
        )

        val belowCustom = mbToBytes(800)
        assertFalse(
            "800 MB should be rejected when custom threshold is 1000 MB",
            MemoryBudgetDecider.shouldLoadLlama(belowCustom, false, customThreshold)
        )
    }
}
