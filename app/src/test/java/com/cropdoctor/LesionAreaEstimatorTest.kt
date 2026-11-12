package com.cropdoctor

import com.cropdoctor.vision.LesionAreaEstimator
import org.junit.Assert.assertEquals
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
            pixels[i] = createPixel(35, 155, 35) // Green
        }
        for (i in half until total) {
            pixels[i] = createPixel(180, 180, 25) // Chlorosis yellow
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
}

