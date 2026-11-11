package com.cropdoctor.advisor

import java.util.regex.Pattern

object GuardrailFilter {

    // Regex detecting unverified dosage numbers and rates (e.g. 50 ml/L, 2 kg per acre, 2.5 g/l)
    private val DOSE_PATTERN = Pattern.compile(
        "\\b\\d+(\\.\\d+)?\\s*(ml|g|gm|mg|l|litre|liter|kg)\\s*(per|/|in)\\s*(\\d*\\s*)?(l|litre|liter|acre|ha|hectare|water)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val REPLACEMENT = "follow the product label"

    /**
     * Sanitizes Llama output by scrubbing any hallucinated dosage numbers or application rates,
     * replacing them with official directive to follow the CIB&RC product label.
     */
    fun sanitize(text: String): String {
        val matcher = DOSE_PATTERN.matcher(text)
        return matcher.replaceAll(REPLACEMENT)
    }
}

