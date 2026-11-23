package com.cropdoctor.chat

/**
 * Enforces strict grounding for Llama 3.2 1B generated chat answers (Task CHAT, Rule H4 & H5).
 *
 * Rules:
 * 1. The answer must ONLY use information from the retrieved passages and the Finding.
 * 2. Rejects any dose rate (e.g. ml/L, g/L, kg/ha) not present in the passages.
 * 3. Rejects any chemical active ingredient or product not present in the passages.
 * 4. Rejects any numbers (e.g. concentrations, percentages, counts) not present in the passages or finding.
 */
object ChatGuardrail {

    data class ValidationResult(
        val isValid: Boolean,
        val failureReason: String? = null
    )

    private val DOSE_UNIT_REGEX = Regex(
        """\b(\d+(?:\.\d+)?\s*(?:ml\/l|g\/l|kg\/ha|ppm|ml|g|gm|kg|litres?|liters?|లక్ష|మి\.లీ|గ్రా|కిలో|लीटर|ग्राम|मिली))\b""",
        RegexOption.IGNORE_CASE
    )

    private val NUMBER_REGEX = Regex("""\b(\d+(?:\.\d+)?%?)\b""")

    private val KNOWN_CHEMICAL_ACTIVES = listOf(
        "tricyclazole", "mancozeb", "copper oxychloride", "streptocycline",
        "chlorantraniliprole", "chlorpyrifos", "propiconazole", "spinetoram",
        "emamectin benzoate", "emamectin", "diafenthiuron", "fipronil",
        "azoxystrobin", "difenoconazole", "carbendazim", "thiram", "imidacloprid",
        "monocrotophos", "dimethoate", "acephate", "cypermethrin", "malathion", "ddt", "endosulfan",
        "మోనోక్రోటోఫాస్", "క్లోరాంట్రానిలిప్రోల్", "ట్రైసైక్లజోల్", "మాంకోజెబ్", "క్లోర్‌పైరిఫాస్",
        "मोनोक्रोटोफॉस", "क्लोरांट्रानिलीप्रोल", "ट्राइसाइक्लाजोल", "मैंकोजेब", "क्लोरपायरीफॉस"
    )

    fun validate(
        generatedAnswer: String,
        passages: List<Passage>,
        findingText: String
    ): ValidationResult {
        val answer = generatedAnswer.trim()
        if (answer.isBlank()) {
            return ValidationResult(false, "Generated answer is empty")
        }

        val passagesCombined = passages.joinToString(" ") { it.text }
        val referenceText = "$passagesCombined $findingText"

        // 1. Check for ungrounded chemical products or active ingredients
        val lowerAnswer = answer.lowercase()
        val lowerRef = referenceText.lowercase()
        for (chem in KNOWN_CHEMICAL_ACTIVES) {
            if (lowerAnswer.contains(chem) && !lowerRef.contains(chem)) {
                return ValidationResult(
                    isValid = false,
                    failureReason = "Ungrounded chemical product '$chem' not in retrieved passages"
                )
            }
        }

        // 2. Check for ungrounded dose rates
        val answerDoseMatches = DOSE_UNIT_REGEX.findAll(answer).map { it.value.trim().lowercase() }.toList()
        for (dose in answerDoseMatches) {
            val normalizedDose = dose.replace(" ", "")
            val normalizedRef = referenceText.lowercase().replace(" ", "")
            if (!normalizedRef.contains(normalizedDose)) {
                return ValidationResult(
                    isValid = false,
                    failureReason = "Ungrounded dose rate '$dose' not in retrieved passages"
                )
            }
        }

        // 3. Check for ungrounded numbers (excluding bullet indices 1, 2, 3)
        val answerNumbers = NUMBER_REGEX.findAll(answer)
            .map { it.groupValues[1] }
            .filterNot { it in listOf("1", "2", "3") } // allow small bullet step numbering
            .toList()

        val refNumbers = NUMBER_REGEX.findAll(referenceText)
            .map { it.groupValues[1] }
            .toSet()

        for (num in answerNumbers) {
            val rawNum = num.trimEnd('%')
            val refHasRaw = refNumbers.any { it.trimEnd('%') == rawNum }
            if (!refHasRaw) {
                return ValidationResult(
                    isValid = false,
                    failureReason = "Ungrounded number '$num' not present in retrieved passages"
                )
            }
        }

        return ValidationResult(isValid = true)
    }
}
