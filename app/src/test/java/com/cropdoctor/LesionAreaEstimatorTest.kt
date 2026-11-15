package com.cropdoctor

import com.cropdoctor.vision.LesionAreaEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LesionAreaEstimatorTest {

    private fun createPixel(r: Int, g: Int, b: Int): Int {
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    @Test
    fun testHealthyGreenFoliageYieldsZeroLesions() {
        val total = 64 * 64
        val greenPixels = IntArray(total) { createPixel(35, 155, 35) }
        val lesion = LesionAreaEstimator.estimateFromPixels(greenPixels)
        assertEquals(0.0f, lesion, 0.001f)
    }

    @Test
    fun testChloroticYellowSymptomsDetected() {
        val total = 64 * 64
        val pixels = IntArray(total)
        val half = total / 2
        for (i in 0 until half) {
            pixels[i] = createPixel(35, 155, 35) // Green foliage
        }
        for (i in half until total) {
            pixels[i] = createPixel(190, 130, 25) // Chlorosis amber-yellow (hue ~38°)
        }
        val lesion = LesionAreaEstimator.estimateFromPixels(pixels)
        assertTrue("Lesion fraction should be significant for 50% chlorosis: $lesion", lesion >= 0.30f)
    }

    @Test
    fun testNecroticBrownSpotsDetected() {
        val total = 64 * 64
        val pixels = IntArray(total)
        val necroticCount = (total * 0.25).toInt()
        for (i in 0 until total) {
            if (i < necroticCount) {
                pixels[i] = createPixel(110, 65, 35) // Necrotic brown
            } else {
                pixels[i] = createPixel(40, 160, 40) // Green
            }
        }
        val lesion = LesionAreaEstimator.estimateFromPixels(pixels)
        assertTrue("Lesion fraction should be detected for 25% necrosis: $lesion", lesion >= 0.20f)
    }

    @Test
    fun testRustOrangePustulesDetected() {
        val total = 64 * 64
        val pixels = IntArray(total)
        val rustCount = (total * 0.20).toInt()
        for (i in 0 until total) {
            if (i < rustCount) {
                pixels[i] = createPixel(175, 75, 20) // Rust orange
            } else {
                pixels[i] = createPixel(40, 160, 40) // Green
            }
        }
        val lesion = LesionAreaEstimator.estimateFromPixels(pixels)
        assertTrue("Lesion fraction should be detected for 20% rust: $lesion", lesion >= 0.15f)
    }

    // Task L1 Synthetic Unit Test 1: A fully green leaf gives under 2%
    @Test
    fun testSyntheticFullyGreenLeafYieldsUnderTwoPercent() {
        val total = 64 * 64
        val greenPixels = IntArray(total) { createPixel(35, 155, 35) }
        val result = LesionAreaEstimator.estimateLesion(greenPixels)
        assertTrue("Leaf should be detected", result.isLeafFound)
        assertEquals("Status should indicate leaf detected", "leaf detected", result.status)
        assertTrue("Fraction should be under 2% (< 0.02f): ${result.fraction}", result.fraction < 0.02f)
    }

    // Task L1 Synthetic Unit Test 2: A leaf with about 20% brown patch gives about 20% (within 5 points: 15% - 25%)
    @Test
    fun testSyntheticLeafWithTwentyPercentBrownPatchYieldsAboutTwentyPercent() {
        val total = 64 * 64
        val pixels = IntArray(total)
        val brownCount = (total * 0.20).toInt()
        for (i in 0 until total) {
            if (i < brownCount) {
                pixels[i] = createPixel(110, 65, 35) // Necrotic brown patch
            } else {
                pixels[i] = createPixel(40, 160, 40) // Healthy green leaf
            }
        }
        val result = LesionAreaEstimator.estimateLesion(pixels)
        assertTrue("Leaf should be detected", result.isLeafFound)
        assertEquals("Status should indicate leaf detected", "leaf detected", result.status)
        assertTrue(
            "Fraction should be about 20% (within 5 points, i.e. 0.15f..0.25f): ${result.fraction}",
            result.fraction in 0.15f..0.25f
        )
    }

    // Task L1 Synthetic Unit Test 3: A plain background gives "no leaf found"
    @Test
    fun testSyntheticPlainBackgroundYieldsNoLeafFound() {
        val total = 64 * 64

        // Neutral gray desk
        val grayBackground = IntArray(total) { createPixel(128, 128, 128) }
        val grayResult = LesionAreaEstimator.estimateLesion(grayBackground)
        assertFalse("Gray background should not be identified as leaf", grayResult.isLeafFound)
        assertEquals("no leaf found", grayResult.status)
        assertEquals(0.0f, grayResult.fraction, 0.0f)

        // White paper background
        val whiteBackground = IntArray(total) { createPixel(245, 245, 245) }
        val whiteResult = LesionAreaEstimator.estimateLesion(whiteBackground)
        assertFalse("White background should not be identified as leaf", whiteResult.isLeafFound)
        assertEquals("no leaf found", whiteResult.status)
        assertEquals(0.0f, whiteResult.fraction, 0.0f)

        // Dark background / shadow
        val darkBackground = IntArray(total) { createPixel(20, 20, 20) }
        val darkResult = LesionAreaEstimator.estimateLesion(darkBackground)
        assertFalse("Dark background should not be identified as leaf", darkResult.isLeafFound)
        assertEquals("no leaf found", darkResult.status)
        assertEquals(0.0f, darkResult.fraction, 0.0f)
    }
}
