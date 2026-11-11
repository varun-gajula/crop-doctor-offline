package com.cropdoctor.domain

object SeverityCalculator {

    /**
     * Calculates the disease severity band from estimated lesion area fraction
     * and specific pathology rules.
     *
     * @param lesionFraction Estimated fraction of leaf area affected (0.0 to 1.0)
     * @param classId Class identifier (e.g. "rice_bacterial_leaf_blight")
     * @return SeverityBand (MILD, MODERATE, or SEVERE)
     */
    fun calculate(lesionFraction: Float, classId: String): SeverityBand {
        // Healthy classes have zero disease severity
        if (classId.endsWith("_healthy")) {
            return SeverityBand.MILD
        }

        // Virus diseases (leaf curl) are systemic and vector-driven; minimum severity is MODERATE
        if (classId.contains("leaf_curl") || classId.contains("yellow_leaf_curl")) {
            return if (lesionFraction > 0.30f) {
                SeverityBand.SEVERE
            } else {
                SeverityBand.MODERATE
            }
        }

        // Standard lesion area fraction thresholds calibrated against field datasets
        return when {
            lesionFraction < 0.10f -> SeverityBand.MILD
            lesionFraction <= 0.30f -> SeverityBand.MODERATE
            else -> SeverityBand.SEVERE
        }
    }
}

