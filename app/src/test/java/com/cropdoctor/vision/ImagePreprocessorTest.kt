package com.cropdoctor.vision

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class ImagePreprocessorTest {

    @Test
    fun testBasicMath() {
        val mean = ImagePreprocessor.IMAGENET_MEAN
        val std = ImagePreprocessor.IMAGENET_STD
        assertEquals(0.485f, mean[0], 1e-5f)
        assertEquals(0.456f, mean[1], 1e-5f)
        assertEquals(0.406f, mean[2], 1e-5f)
        assertEquals(0.229f, std[0], 1e-5f)
        assertEquals(0.224f, std[1], 1e-5f)
        assertEquals(0.225f, std[2], 1e-5f)
    }

    @Test
    fun testCalculateInSampleSize() {
        assertEquals(1, ImagePreprocessor.calculateInSampleSize(512, 512, 512))
        assertEquals(1, ImagePreprocessor.calculateInSampleSize(800, 600, 512))
        assertEquals(2, ImagePreprocessor.calculateInSampleSize(2048, 1536, 512))
        assertEquals(4, ImagePreprocessor.calculateInSampleSize(4096, 3072, 512))
    }

    @Test
    fun testPixelNormalizationParity() {
        val size = ImagePreprocessor.TARGET_SIZE
        val area = size * size
        val buf1 = ByteBuffer.allocateDirect(3 * area * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val buf2 = ByteBuffer.allocateDirect(3 * area * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        // Test identical pixels produce max difference 0.0 (< 1e-3)
        val pixels = IntArray(area) { idx ->
            val r = (idx * 7) % 256
            val g = (idx * 13) % 256
            val b = (idx * 17) % 256
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        ImagePreprocessor.normalizePixels(pixels, buf1)
        ImagePreprocessor.normalizePixels(pixels, buf2)

        var maxDiff = 0.0f
        for (i in 0 until 3 * area) {
            val diff = abs(buf1.get(i) - buf2.get(i))
            if (diff > maxDiff) maxDiff = diff
        }

        assertTrue("Identical inputs must have difference < 1e-3, got $maxDiff", maxDiff < 1e-3f)
        assertEquals(0.0f, maxDiff, 1e-6f)
    }

    @Test
    fun testPlanarChannelOrder() {
        val size = ImagePreprocessor.TARGET_SIZE
        val area = size * size
        val buf = ByteBuffer.allocateDirect(3 * area * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        // Pure Red pixel: R=255, G=0, B=0
        val pixels = IntArray(area) { (0xFF shl 24) or (0xFF shl 16) }
        ImagePreprocessor.normalizePixels(pixels, buf)

        val expectedR = (1.0f - ImagePreprocessor.IMAGENET_MEAN[0]) / ImagePreprocessor.IMAGENET_STD[0]
        val expectedG = (0.0f - ImagePreprocessor.IMAGENET_MEAN[1]) / ImagePreprocessor.IMAGENET_STD[1]
        val expectedB = (0.0f - ImagePreprocessor.IMAGENET_MEAN[2]) / ImagePreprocessor.IMAGENET_STD[2]

        assertEquals(expectedR, buf.get(0), 1e-4f)
        assertEquals(expectedG, buf.get(area), 1e-4f)
        assertEquals(expectedB, buf.get(2 * area), 1e-4f)
    }

    @Test
    fun testCalculateViewfinderCrop() {
        // Typical portrait capture: 3000 x 4000 on a phone screen
        val crop4000 = ImagePreprocessor.calculateViewfinderCrop(3000, 4000, 0.37f)
        // boxSize = 4000 * 0.37 = 1480
        assertEquals(1480, crop4000.size)
        assertEquals((3000 - 1480) / 2, crop4000.left)
        assertEquals((4000 - 1480) / 2, crop4000.top)

        // Landscape capture: 4000 x 3000
        val cropLand = ImagePreprocessor.calculateViewfinderCrop(4000, 3000, 0.37f)
        assertEquals(1110, cropLand.size)
        assertEquals((4000 - 1110) / 2, cropLand.left)
        assertEquals((3000 - 1110) / 2, cropLand.top)

        // Smaller dimensions should respect TARGET_SIZE or maxPossible
        val cropSmall = ImagePreprocessor.calculateViewfinderCrop(600, 800, 0.37f)
        // 800 * 0.37 = 296, coerced to TARGET_SIZE (512)
        assertEquals(512, cropSmall.size)
        assertEquals((600 - 512) / 2, cropSmall.left)
        assertEquals((800 - 512) / 2, cropSmall.top)
    }
}
