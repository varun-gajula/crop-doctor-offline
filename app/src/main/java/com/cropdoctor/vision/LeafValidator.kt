package com.cropdoctor.vision

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max

data class LeafValidationResult(
    val isLeaf: Boolean,
    val plantPixelFraction: Float,
    val greenFraction: Float,
    val chlorosisFraction: Float,
    val necrosisFraction: Float,
    val reason: String = ""
)

object LeafValidator {

    /**
     * Inspects bitmap to determine if it contains actual plant/crop foliage.
     * Prevents tables, walls, faces, hands, clothing, paper, and random objects from false diagnoses.
     * Works robustly under both outdoor sunlight and indoor/warm lighting conditions.
     */
    fun validate(bitmap: Bitmap): LeafValidationResult {
        val sampleSize = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)

        val totalPixels = sampleSize * sampleSize
        var plantPixelCount = 0
        var greenCount = 0
        var chlorosisCount = 0
        var necrosisCount = 0
        var skinCount = 0
        var neutralCount = 0

        val hsv = FloatArray(3)

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]        // 0..360
                val sat = hsv[1]        // 0..1
                val value = hsv[2]      // 0..1

                // 1. Monochromatic / Neutral surfaces (white paper, gray floor, dark shadows)
                if (sat < 0.10f || value < 0.08f || (value > 0.95f && sat < 0.15f)) {
                    neutralCount++
                    continue
                }

                // 2. Human skin tone check (Hue 8°..32°, R significantly > G and B, High R)
                val isSkinTone = (hue in 8f..32f) && (r > g + 25) && (g > b) && (r in 110..250)
                if (isSkinTone) {
                    skinCount++
                    continue
                }

                // 3. Excess Green Index ExG = 2G - R - B
                val exG = 2 * g - r - b

                // 4. Healthy Green foliage (outdoor and indoor white-balanced)
                val isGreenPlant = (hue in 55f..180f && (g >= b || exG > -15)) ||
                        (hue in 40f..55f && g > r - 10 && g > b)

                if (isGreenPlant) {
                    greenCount++
                    plantPixelCount++
                }
                // 5. Chlorotic Yellow plant tissue (disease symptoms, halos)
                else if (hue in 32f..65f && r > 70 && g > 70 && b < 140 && (r - g) <= 35) {
                    chlorosisCount++
                    plantPixelCount++
                }
                // 6. Necrotic / Brown lesion foliage (leaf spots, blight, rust)
                else if (hue in 14f..35f && r in 45..190 && g in 25..135 && b < 90 && (r - g) <= 50) {
                    necrosisCount++
                    plantPixelCount++
                }
            }
        }

        val plantFraction = plantPixelCount.toFloat() / totalPixels
        val greenFraction = greenCount.toFloat() / totalPixels
        val chlorosisFraction = chlorosisCount.toFloat() / totalPixels
        val necrosisFraction = necrosisCount.toFloat() / totalPixels
        val skinFraction = skinCount.toFloat() / totalPixels

        // If predominantly skin or neutral background with almost no plant tissue
        val hasLivingFoliage = (greenFraction + chlorosisFraction) >= 0.04f
        val isNotPureSkin = skinFraction < 0.65f

        // A valid crop leaf must have at least 6% plant foliage in the frame
        val isLeaf = hasLivingFoliage && isNotPureSkin && (plantFraction >= 0.06f)

        val reason = if (!isLeaf) {
            "No crop leaf detected in image. Please aim camera at a clear crop leaf."
        } else {
            "Plant foliage verified (${(plantFraction * 100).toInt()}% foliage coverage)"
        }

        return LeafValidationResult(
            isLeaf = isLeaf,
            plantPixelFraction = plantFraction,
            greenFraction = greenFraction,
            chlorosisFraction = chlorosisFraction,
            necrosisFraction = necrosisFraction,
            reason = reason
        )
    }
}
