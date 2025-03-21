package com.example.madeinitaly.extraction

/**
 * Database of substances that are banned or restricted in the EU/Italy
 * Each substance can be identified by multiple names/conventions
 */
object BannedSubstances {
    // Map of banned substances with their various naming conventions
    val bannedSubstances: Map<String, List<String>> = mapOf(
        // Preservatives
        "Potassium bromate" to listOf(
            "potassium bromate", "bromato de potasio", "E924", "E924a", "E-924", "E-924a"
        ),
        "Brominated vegetable oil" to listOf(
            "brominated vegetable oil", "BVO", "vegetable oil, brominated"
        ),
        "Azodicarbonamide" to listOf(
            "azodicarbonamide", "ADA", "azodicarboxamide", "E927", "E-927"
        ),
        "Tertiary butylhydroquinone" to listOf(
            "tertiary butylhydroquinone", "TBHQ", "tert-butylhydroquinone", "E319", "E-319"
        ),
        "Butylated hydroxyanisole" to listOf(
            "butylated hydroxyanisole", "BHA", "E320", "E-320"
        ),
        "Butylated hydroxytoluene" to listOf(
            "butylated hydroxytoluene", "BHT", "E321", "E-321"
        ),

        // Colors
        "Yellow #5" to listOf(
            "yellow #5", "yellow 5", "tartrazine", "E102", "E-102", "FD&C Yellow No. 5", "CI 19140"
        ),
        "Yellow #6" to listOf(
            "yellow #6", "yellow 6", "sunset yellow", "E110", "E-110", "FD&C Yellow No. 6", "CI 15985"
        ),
        "Red #40" to listOf(
            "red #40", "red 40", "allura red", "E129", "E-129", "FD&C Red No. 40", "CI 16035"
        ),
        "Blue #1" to listOf(
            "blue #1", "blue 1", "brilliant blue", "E133", "E-133", "FD&C Blue No. 1", "CI 42090"
        ),
        "Blue #2" to listOf(
            "blue #2", "blue 2", "indigo carmine", "E132", "E-132", "FD&C Blue No. 2", "CI 73015"
        ),
        "Green #3" to listOf(
            "green #3", "green 3", "fast green", "E143", "E-143", "FD&C Green No. 3", "CI 42053"
        ),

        // Other additives
        "Potassium iodate" to listOf(
            "potassium iodate", "KIO3"
        ),
        "Cyclamates" to listOf(
            "cyclamate", "cyclamates", "sodium cyclamate", "calcium cyclamate", "E952", "E-952"
        ),
        "Olestra" to listOf(
            "olestra", "olean"
        ),
        "rBGH" to listOf(
            "rbgh", "rbst", "recombinant bovine growth hormone", "recombinant bovine somatotropin"
        )
    )

    /**
     * Checks if an ingredient text contains any banned substances
     * @param ingredientText Text containing ingredient list
     * @return Pair(Boolean, List<String>) - first: contains banned substance, second: list of found substances
     */
    fun containsBannedSubstances(ingredientText: String): Pair<Boolean, List<String>> {
        val normalizedText = ingredientText.lowercase().trim()
        val foundSubstances = mutableListOf<String>()

        // Special pattern for E-codes which might appear with various delimiters
        val eCodePattern = "\\b(E-?\\d{3}[a-z]?)\\b".toRegex()
        val eCodesInText = eCodePattern.findAll(normalizedText).map { it.groupValues[1] }.toList()

        // Check each banned substance
        bannedSubstances.forEach { (substance, aliases) ->
            // First check if any E-codes in the text match our aliases
            if (aliases.any { alias ->
                    alias.lowercase().matches("e-?\\d{3}[a-z]?".toRegex()) &&
                            eCodesInText.any { eCode -> eCode.replace("-", "").equals(alias.replace("-", ""), ignoreCase = true) }
                }) {
                foundSubstances.add(substance)
            }
            // Then check other aliases with word boundaries
            else if (aliases.any { alias ->
                    val aliasRegex = "\\b${Regex.escape(alias.lowercase())}\\b".toRegex()
                    aliasRegex.containsMatchIn(normalizedText)
                }) {
                foundSubstances.add(substance)
            }
        }

        return Pair(foundSubstances.isNotEmpty(), foundSubstances)
    }
}