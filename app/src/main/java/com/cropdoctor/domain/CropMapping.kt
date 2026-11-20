package com.cropdoctor.domain

/**
 * Task P3: Class-to-Crop mapping derived from labels.json prefixes.
 *
 * Sourced crops: Rice, Cotton, Chilli, Tomato, Maize.
 * Every class in labels.json belongs to exactly one crop, except weeds_not_a_crop (non-crop).
 */
object CropMapping {

    val VALID_CROPS = listOf("rice", "cotton", "chilli", "tomato", "maize")
    val SUPPORTED_CROPS: List<String> get() = VALID_CROPS

    /**
     * Returns the indices of classes in labels that belong to the given crop.
     */
    fun getClassIndicesForCrop(crop: String, labels: List<String>): List<Int> {
        val normalizedCrop = crop.trim().lowercase()
        return labels.indices.filter { isClassOfCrop(labels[it], normalizedCrop) }
    }

    /**
     * Determines the crop prefix from a classId in labels.json.
     * Every class has exactly one crop except "weeds_not_a_crop", which returns null.
     */
    fun getCropForClass(classId: String): String? {
        val normalized = classId.trim().lowercase()
        if (normalized == "weeds_not_a_crop" || !normalized.contains("_")) {
            return null
        }
        val prefix = normalized.substringBefore("_")
        return if (prefix in VALID_CROPS) prefix else null
    }

    /**
     * Checks if a class belongs to a specific crop.
     */
    fun isClassOfCrop(classId: String, crop: String): Boolean {
        val normalizedCrop = crop.trim().lowercase()
        return getCropForClass(classId) == normalizedCrop
    }
}
