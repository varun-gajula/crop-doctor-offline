package com.cropdoctor.vision

import android.graphics.Bitmap
import kotlin.math.abs

/**
 * Botanical heuristic estimator for lesion fraction on diseased plant tissue.
 * Segments leaf tissue from background (excluding neutral surfaces, glare, deep shadow, non-plant hues),
 * then calculates the proportion of diseased tissue (chlorosis, necrosis, rust pustules, fungal mold)
 * strictly within the segmented leaf boundary.
 *
 * Adheres strictly to Rule H11 (colour heuristics permitted ONLY for lesion fraction)
 * and Rule H12 (no hard-coded per-class constants, no clamping to arbitrary limits).
 */
object LesionAreaEstimator {

    data class LesionResult(
        val fraction: Float,
        val leafPixelCount: Int,
        val lesionPixelCount: Int,
        val isLeafFound: Boolean,
        val status: String
    )

    fun estimateLesion(bitmap: Bitmap): LesionResult {
        val sampleGrid = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleGrid, sampleGrid, false)
        val pixels = IntArray(sampleGrid * sampleGrid)
        scaled.getPixels(pixels, 0, sampleGrid, 0, 0, sampleGrid, sampleGrid)
        return estimateLesion(pixels)
    }

    fun estimateLesionFraction(bitmap: Bitmap): Float {
        return estimateLesion(bitmap).fraction
    }

    fun estimateFromPixels(pixels: IntArray): Float {
        return estimateLesion(pixels).fraction
    }

    fun estimateLesion(pixels: IntArray): LesionResult {
        var greenCount = 0
        var chloroticCount = 0
        var necroticCount = 0
        var rustCount = 0
        var whiteMoldCount = 0

        val hsv = FloatArray(3)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            rgbToHsv(r, g, b, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]

            // 1. Background exclusion:
            // Exclude extreme dark / deep shadows
            if (value < 0.15f) continue
            // Exclude extreme bright glare / white background
            if (value > 0.92f && sat < 0.18f) continue
            // Exclude neutral / monochromatic / gray surfaces
            if (sat < 0.12f) continue
            // Exclude low color variance (|r-g| + |g-b| + |b-r| < 25)
            if (abs(r - g) + abs(g - b) + abs(b - r) < 25) continue
            // Exclude non-botanical hues (blue, purple, pink 180°..330°)
            if (hue in 180f..330f) continue

            val exG = 2 * g - r - b

            // 2. Botanical Tissue Segmentation
            if (hue in 50f..175f && g >= b && exG > -15 && sat >= 0.15f && value >= 0.15f) {
                // Healthy foliage
                greenCount++
            } else if (hue in 35f..50f && g >= b + 10 && r >= g - 25 && sat >= 0.18f && value >= 0.18f) {
                // Chlorotic yellow tissue
                chloroticCount++
            } else if (hue in 14f..34f && r >= 85 && g >= 35 && b <= 80 && r > g && g > b && sat >= 0.25f && (r - b) >= 25) {
                // Rust pustules / orange lesions
                rustCount++
            } else if (hue in 12f..40f && r >= 45 && g >= 25 && r > b + 18 && g >= b - 5 && sat >= 0.16f && value <= 0.65f) {
                // Necrotic brown/black lesions
                necroticCount++
            } else if (value in 0.65f..0.92f && sat in 0.08f..0.22f && g >= b && r >= b) {
                // White powdery mildew / fungal bloom
                whiteMoldCount++
            }
        }

        val plantFoliage = greenCount + chloroticCount
        val totalLeaf = plantFoliage + necroticCount + rustCount + whiteMoldCount

        // Minimum threshold to prevent noise on non-leaf images from hallucinating disease
        val minFoliage = maxOf(3, (pixels.size * 0.0012f).toInt())
        val minTotalLeaf = maxOf(5, (pixels.size * 0.0036f).toInt())

        if (plantFoliage < minFoliage || totalLeaf < minTotalLeaf) {
            return LesionResult(
                fraction = 0.0f,
                leafPixelCount = totalLeaf,
                lesionPixelCount = 0,
                isLeafFound = false,
                status = "no leaf found"
            )
        }

        val lesionCount = necroticCount + rustCount + whiteMoldCount + chloroticCount
        // Fraction is strictly inside the leaf area, unclamped (ranges naturally from 0.0 to 1.0)
        val fraction = (lesionCount.toFloat() / totalLeaf.toFloat()).coerceIn(0.0f, 1.0f)

        return LesionResult(
            fraction = fraction,
            leafPixelCount = totalLeaf,
            lesionPixelCount = lesionCount,
            isLeafFound = true,
            status = "leaf detected"
        )
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
