package com.cropdoctor.vision

import android.graphics.Bitmap

data class LeafValidationResult(
    val isLikelyCrop: Boolean,
    val plantPixelFraction: Float,
    val reason: String = ""
)

object LeafValidator {

    /**
     * Inspects an image to check if it contains agricultural crop tissue:
     * leaves, stems, fruits (tomatoes, chillies), cotton bolls, cobs, or pests.
     *
     * Returns a SOFT advisory result: isLikelyCrop = false only when the image
     * is predominantly human skin (hands/faces) or non-agricultural neutral surfaces (desks/walls).
     *
     * Never blocks diagnosis; the UI allows the farmer to "Use this photo anyway".
     */
    fun validate(bitmap: Bitmap): LeafValidationResult {
        val sampleSize = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)
        val pixels = IntArray(sampleSize * sampleSize)
        scaled.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)
        return validatePixels(sampleSize, sampleSize, pixels)
    }

    fun validatePixels(width: Int, height: Int, pixels: IntArray): LeafValidationResult {
        val totalPixels = width * height
        if (totalPixels == 0) {
            return LeafValidationResult(false, 0f, "Empty image")
        }

        var cropPixelCount = 0
        var skinCount = 0
        var neutralCount = 0

        val hsv = FloatArray(3)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            rgbToHsv(r, g, b, hsv)
            val hue = hsv[0]        // 0..360
            val sat = hsv[1]        // 0..1
            val value = hsv[2]      // 0..1

            // 1. Human skin tone check (Hue 8°..32°, R > G+20, G > B, high brightness value > 0.55, R in 140..250)
            val isSkinTone = (hue in 8f..32f) && (r > g + 20) && (g > b) && (r in 140..250) && (value > 0.55f) && (sat in 0.15f..0.60f)
            if (isSkinTone) {
                skinCount++
                continue
            }

            // 2. Cotton boll / white-cream lint check
            val isCottonBoll = (sat < 0.22f && value > 0.70f && r in 170..255 && g in 170..255 && b in 150..255)
            if (isCottonBoll) {
                cropPixelCount++
                continue
            }

            // 3. Monochromatic / Neutral background (gray desk, shadow, blackboard)
            if (sat < 0.10f || value < 0.08f || (value > 0.95f && sat < 0.12f)) {
                neutralCount++
                continue
            }

            val exG = 2 * g - r - b

            // 4. Healthy Green foliage
            val isGreenPlant = (hue in 55f..180f && (g >= b || exG > -15)) ||
                    (hue in 40f..55f && g > r - 10 && g > b)

            // 5. Chlorotic Yellow plant tissue / Maize cob
            val isYellowCrop = (hue in 32f..65f && r > 65 && g > 65 && b < 160)

            // 6. Necrotic / Brown diseased leaf tissue or insect body
            val isBrownCrop = (hue in 14f..38f && r in 40..200 && g in 20..150 && b < 120 && (r - g) <= 60)

            // 7. Red fruits: Tomatoes and Chillies (Hue near 0° or 360°)
            val isRedFruit = ((hue in 0f..20f) || (hue in 340f..360f)) && (r > 85 && r > g + 15 && r > b + 15)

            if (isGreenPlant || isYellowCrop || isBrownCrop || isRedFruit) {
                cropPixelCount++
            }
        }

        val cropFraction = cropPixelCount.toFloat() / totalPixels
        val skinFraction = skinCount.toFloat() / totalPixels

        // Soft warning triggers only if overwhelmingly skin or almost completely devoid of crop features
        val isLikelyCrop = when {
            skinFraction > 0.40f && cropFraction < 0.08f -> false
            cropFraction < 0.04f -> false
            else -> true
        }

        val reason = if (!isLikelyCrop) {
            "This may not be a crop photo"
        } else {
            "Crop subject detected (${(cropFraction * 100).toInt()}% coverage)"
        }

        return LeafValidationResult(
            isLikelyCrop = isLikelyCrop,
            plantPixelFraction = cropFraction,
            reason = reason
        )
    }

    private fun rgbToHsv(r: Int, g: Int, b: Int, hsv: FloatArray) {
        val rf = r / 255.0f
        val gf = g / 255.0f
        val bf = b / 255.0f

        val maxVal = maxOf(rf, gf, bf)
        val minVal = minOf(rf, gf, bf)
        val delta = maxVal - minVal

        hsv[2] = maxVal
        hsv[1] = if (maxVal == 0f) 0f else delta / maxVal

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
