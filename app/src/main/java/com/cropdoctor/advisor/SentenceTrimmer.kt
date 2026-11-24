package com.cropdoctor.advisor

/**
 * Ensures generated text ends on a complete sentence (Rule H5 & Task LLAMA-COMPLETE).
 * Terminators supported: '.', '!', '?', and Hindi purna viram (।) \u0964.
 */
object SentenceTrimmer {
    private val SENTENCE_END_CHARS = charArrayOf('.', '!', '?', '।', '\u0964')
    private val SPECIAL_TOKEN_REGEX = Regex("<\\|[a-zA-Z0-9_]+\\|>")

    /**
     * Checks if the text ends on a complete sentence.
     * If not, trims to the last complete sentence terminator.
     * Special tokens like <|eot_id|> are stripped before sentence boundary detection.
     *
     * @return Pair of (cleanText, wasCut). If no terminator exists, returns Pair("", true).
     */
    fun cleanToCompleteSentence(text: String, language: String = "en"): Pair<String, Boolean> {
        val noSpecial = SPECIAL_TOKEN_REGEX.replace(text, "")
        val trimmed = noSpecial.trim()
        if (trimmed.isEmpty()) return Pair("", false)

        val lastChar = trimmed.last()
        // If it ends with a terminator, or terminator followed by closing quote/bracket
        if (lastChar in SENTENCE_END_CHARS) {
            return Pair(trimmed, false)
        }
        if (trimmed.length >= 2 && trimmed[trimmed.length - 2] in SENTENCE_END_CHARS && (lastChar == '"' || lastChar == '\'' || lastChar == ')')) {
            return Pair(trimmed, false)
        }

        // Search backwards for the last sentence terminator
        var bestIndex = -1
        for (i in trimmed.indices.reversed()) {
            val ch = trimmed[i]
            if (ch in SENTENCE_END_CHARS) {
                // If it's a period following a bullet index like " 3." or "\n1.", skip it
                if (ch == '.' && i > 0 && trimmed[i - 1].isDigit()) {
                    var j = i - 1
                    while (j >= 0 && trimmed[j].isDigit()) {
                        j--
                    }
                    if (j < 0 || trimmed[j].isWhitespace()) {
                        // This is a list numbering prefix like "1.", "2.", "3."
                        continue
                    }
                }
                bestIndex = i
                break
            }
        }

        return if (bestIndex >= 0) {
            val cut = trimmed.substring(0, bestIndex + 1).trim()
            Pair(cut, true)
        } else {
            Pair("", true)
        }
    }
}
