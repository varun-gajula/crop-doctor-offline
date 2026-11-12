package com.cropdoctor.vision

import android.graphics.Bitmap
import kotlin.math.max

/**
 * Pure botanical heuristic estimator for lesion fraction on diseased plant tissue.
 * Calculates the proportion of yellow chlorosis, brown/dark necrotic spots, rust orange pustules,
 * and white fungal powdery lesions relative to total foliage pixels.
 *
 * Adheres strictly to Rule H11 (colour heuristics permitted ONLY for lesion fraction).
 */
object LesionAreaEstimator {

    fun estimateLesionFraction(bitmap: Bitmap): Float {
        val sampleGrid = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleGrid, sampleGrid, false)
        val pixels = IntArray(sampleGrid * sampleGrid)
        scaled.getPixels(pixels, 0, sampleGrid, 0, 0, sampleGrid, sampleGrid)
        return estimateFromPixels(pixels)
    }

    fun estimateFromPixels(pixels: IntArray): Float {
        var darkSpotCount = 0
        var rustOrangeCount = 0
        var whiteMoldCount = 0
        var yellowCount = 0
        var greenCount = 0
        var totalFoliageCount = 0

        val hsv = FloatArray(3)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            rgbToHsv(r, g, b, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]

            // Ignore neutral / monochromatic background
            if (sat < 0.10f || value < 0.08f || (value > 0.95f && sat < 0.15f)) {
                continue
            }

            val exG = 2 * g - r - b
            val isPlant = (hue in 55f..180f && (g >= b || exG > -15)) ||
                    (hue in 35f..55f && g > r - 15) ||
                    (hue in 14f..35f && r in 45..210 && g in 25..140 && (r - g) <= 120)

            if (isPlant) {
                totalFoliageCount++

                if (hue in 65f..175f && g >= r - 10) {
                    greenCount++
                } else if (hue in 35f..65f) {
                    yellowCount++
                } else if (hue in 16f..34f && r > 125 && g in 45..115 && b < 55) {
                    rustOrangeCount++
                } else if (hue in 14f..35f && r in 45..190 && (r - g) <= 50) {
                    darkSpotCount++
                } else if (value > 0.75f && sat < 0.20f) {
                    whiteMoldCount++
                }
            }
        }

        if (totalFoliageCount == 0) return 0.0f

        val foliageTotal = totalFoliageCount.toFloat()
        val yellowRatio = yellowCount / foliageTotal
        val spotRatio = darkSpotCount / foliageTotal
        val rustRatio = rustOrangeCount / foliageTotal
        val whiteRatio = whiteMoldCount / foliageTotal

        return (yellowRatio * 0.7f + spotRatio * 1.2f + rustRatio * 1.0f + whiteRatio * 0.8f).coerceIn(0.0f, 0.95f)
    }

    private fun rgbToHsv(r: Int, g: Int, b: Int, hsv: FloatArray) {
        val rf = r / 255.0f
        val gf = g / 255.0f
        val bf = b / 255.0f

        val maxVal = maxOf(rf, gf, bf)
        val minVal = minOf(rf, gf, bf)
        val delta = maxVal - minVal

        // Value
        hsv[2] = maxVal

        // Saturation
        hsv[1] = if (maxVal == 0f) 0f else delta / maxVal

        // Hue
        if (delta == 0f) {
            hsv[0] = 0f
        } else {
            var h = when (maxVal) {
                rf -> ((gf - bf) / delta) % 6f
                gf -> ((bf - rf) / delta) + 2f
                else -> ((rf - gf) / delta) + 4f
            } * 60f
            if (h < 0f) h += 360f
            hsv[0] = h
        }
    }
}

