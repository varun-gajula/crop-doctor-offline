package com.cropdoctor

import com.cropdoctor.vision.LeafValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeafValidatorTest {

    private fun pixel(r: Int, g: Int, b: Int): Int {
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    @Test
    fun testHealthyGreenLeaf_accepted() {
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(35, 160, 40) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertTrue("Healthy green leaf must be accepted", result.isLikelyCrop)
    }

    @Test
    fun testBrownDiseasedLeaf_neverBlocked() {
        // Badly diseased necrotic brown leaf
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(120, 75, 40) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertTrue("Badly diseased brown leaf must never be blocked", result.isLikelyCrop)
    }

    @Test
    fun testTomatoFruitRed_neverBlocked() {
        // Red tomato fruit
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(210, 40, 30) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertTrue("Tomato fruit must never be blocked", result.isLikelyCrop)
    }

    @Test
    fun testChilliFruitRed_neverBlocked() {
        // Red chilli pepper
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(195, 30, 25) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertTrue("Chilli fruit must never be blocked", result.isLikelyCrop)
    }

    @Test
    fun testCottonBoll_neverBlocked() {
        // Cotton boll with white lint fibers
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(235, 235, 230) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertTrue("Cotton boll must never be blocked", result.isLikelyCrop)
    }

    @Test
    fun testPestInsect_neverBlocked() {
        // Close up of insect/pest on leaf background
        val total = 64 * 64
        val pixels = IntArray(total)
        val insectCount = (total * 0.3).toInt()
        for (i in 0 until total) {
            if (i < insectCount) {
                pixels[i] = pixel(70, 45, 25) // Brown insect body
            } else {
                pixels[i] = pixel(50, 150, 50) // Plant background
            }
        }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertTrue("Pest/insect on crop must never be blocked", result.isLikelyCrop)
    }

    @Test
    fun testMaizeCob_neverBlocked() {
        // Yellow/golden maize cob
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(220, 190, 45) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertTrue("Maize cob must never be blocked", result.isLikelyCrop)
    }

    @Test
    fun testHumanSkinHand_triggersSoftWarning() {
        // Predominantly human hand / skin
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(220, 160, 130) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertFalse("Human hand should trigger soft warning", result.isLikelyCrop)
    }

    @Test
    fun testNeutralDeskSurface_triggersSoftWarning() {
        // Gray neutral office desk
        val total = 64 * 64
        val pixels = IntArray(total) { pixel(128, 128, 128) }
        val result = LeafValidator.validatePixels(64, 64, pixels)
        assertFalse("Neutral desk surface should trigger soft warning", result.isLikelyCrop)
    }
}

