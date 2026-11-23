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
     * Works with any labels.json (both 20-class and 15-class models).
     * Returns null for non-crop classes (weeds_not_a_crop, other_not_a_crop, or labels without '_').
     */
    fun getCropForClass(classId: String): String? {
        val normalized = classId.trim().lowercase()
        if (normalized == "weeds_not_a_crop" || normalized == "other_not_a_crop" || !normalized.contains("_")) {
            return null
        }
        return normalized.substringBefore("_")
    }

    /**
     * Task AUTO: Computes total probability mass per crop across all model classes.
     */
    fun computeCropMasses(labels: List<String>, probabilities: FloatArray): Map<String, Float> {
        val masses = mutableMapOf<String, Float>()
        val count = minOf(labels.size, probabilities.size)
        for (i in 0 until count) {
            val crop = getCropForClass(labels[i])
            if (crop != null) {
                masses[crop] = (masses[crop] ?: 0.0f) + probabilities[i]
            }
        }
        return masses
    }

    /**
     * Checks if a class belongs to a specific crop.
     */
    fun isClassOfCrop(classId: String, crop: String): Boolean {
        val normalizedCrop = crop.trim().lowercase()
        return getCropForClass(classId) == normalizedCrop
    }

    /**
     * Resolves localized display name for a crop key from strings.xml.
     */
    fun getCropDisplayName(cropKey: String?, context: android.content.Context): String {
        return when (cropKey?.trim()?.lowercase()) {
            "rice" -> context.getString(com.cropdoctor.R.string.crop_rice)
            "cotton" -> context.getString(com.cropdoctor.R.string.crop_cotton)
            "chilli" -> context.getString(com.cropdoctor.R.string.crop_chilli)
            "tomato" -> context.getString(com.cropdoctor.R.string.crop_tomato)
            "maize" -> context.getString(com.cropdoctor.R.string.crop_maize)
            else -> cropKey?.replaceFirstChar { it.uppercase() } ?: context.getString(com.cropdoctor.R.string.crop_auto)
        }
    }
}
