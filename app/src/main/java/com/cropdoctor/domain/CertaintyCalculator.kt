package com.cropdoctor.domain

object CertaintyCalculator {

    /**
     * Maps raw softmax / calibrated probability to farmer-readable certainty bands.
     * High: >= 0.80 ("Sure" / "ఖచ్చితంగా ఉంది" / "निश्चित")
     * Medium: 0.60 - 0.79 ("Probably" / "బహుశా" / "संभवतः")
     * Low: < 0.60 ("Not sure" / "ఖచ్చితంగా లేదు" / "संदेहजनक")
     */
    fun calculate(confidence: Float): CertaintyBand {
        return when {
            confidence >= 0.80f -> CertaintyBand.SURE
            confidence >= 0.60f -> CertaintyBand.PROBABLY
            else -> CertaintyBand.NOT_SURE
        }
    }
}
