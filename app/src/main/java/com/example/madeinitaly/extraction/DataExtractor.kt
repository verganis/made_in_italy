package com.example.madeinitaly.extraction

import com.example.madeinitaly.data.ProductDataModel
import java.util.regex.Pattern

object DataExtractor {
    // Italian product certification marks
    private val CERT_PATTERNS = mapOf(
        "DOP" to Pattern.compile("(?i)\\b(DOP|D\\.O\\.P|Denominazione di Origine Protetta)\\b"),
        "IGP" to Pattern.compile("(?i)\\b(IGP|I\\.G\\.P|Indicazione Geografica Protetta)\\b"),
        "DOCG" to Pattern.compile("(?i)\\b(DOCG|D\\.O\\.C\\.G)\\b"),
        "DOC" to Pattern.compile("(?i)\\b(DOC|D\\.O\\.C)\\b"),
        "STG" to Pattern.compile("(?i)\\b(STG|S\\.T\\.G|Specialità Tradizionale Garantita)\\b"),
        "BIO" to Pattern.compile("(?i)\\b(BIO|Biologico|Organic|Organico)\\b")
    )

    // Serial number patterns (simplified)
    private val SERIAL_NUMBER_PATTERN = Pattern.compile("(?i)(serial|s/n|series|code)[:\\s]*(\\w{5,})")

    // Date patterns
    private val DATE_PATTERN = Pattern.compile("(?i)(prod|mfg|manufacturing|production)[ :.-]*(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4})")

    // "Made in Italy" pattern
    private val MADE_IN_ITALY_PATTERN = Pattern.compile("(?i)(made in italy|prodotto in italia|fabbricato in italia)")

    fun extractProductData(text: String, labels: List<Pair<String, Float>>): ProductDataModel {
        val certifications = extractCertifications(text)
        val serialNumber = extractSerialNumber(text)
        val productionDate = extractProductionDate(text)
        val madeInItaly = MADE_IN_ITALY_PATTERN.matcher(text).find()

        // Calculate confidence based on detected labels
        val italianConfidence = calculateItalianConfidence(labels)

        return ProductDataModel(
            id = System.currentTimeMillis().toString(),
            name = extractProductName(text, labels),
            manufacturer = extractManufacturerName(text),
            certifications = certifications,
            productionDate = productionDate ?: "",
            serialNumber = serialNumber ?: "",
            productionLocation = if (madeInItaly) "Italy" else "",
            confidenceScore = italianConfidence
        )
    }

    private fun extractCertifications(text: String): List<String> {
        return CERT_PATTERNS.mapNotNull { (cert, pattern) ->
            if (pattern.matcher(text).find()) cert else null
        }
    }

    private fun extractSerialNumber(text: String): String? {
        val matcher = SERIAL_NUMBER_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group(2) else null
    }

    private fun extractProductionDate(text: String): String? {
        val matcher = DATE_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group(2) else null
    }

    private fun extractProductName(text: String, labels: List<Pair<String, Float>>): String {
        // This is a simplified approach - in a real app, more sophisticated NLP would be needed
        val firstLines = text.split("\n").take(3)
        val potentialNames = firstLines.filter { it.length in 3..50 }

        return if (potentialNames.isNotEmpty()) {
            potentialNames.first()
        } else if (labels.isNotEmpty()) {
            // Fallback to the most confident label
            labels.maxByOrNull { it.second }?.first ?: ""
        } else {
            ""
        }
    }

    private fun extractManufacturerName(text: String): String {
        // This is a simplified approach - in a real app, more sophisticated NLP would be needed
        // Looking for common patterns like "by CompanyName" or "CompanyName srl/spa"
        val manufacturerPatterns = listOf(
            Pattern.compile("(?i)by\\s+([A-Z][A-Za-z\\s]{2,}?)\\b"),
            Pattern.compile("(?i)([A-Z][A-Za-z\\s]{2,}?)\\s+(srl|spa|s\\.p\\.a\\.)[^\\w]")
        )

        for (pattern in manufacturerPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1).trim()
            }
        }

        return ""
    }

    private fun calculateItalianConfidence(labels: List<Pair<String, Float>>): Float {
        val italianIndicators = listOf("italian", "italy", "made in italy", "handcrafted", "artisan")
        var totalScore = 0f
        var matchCount = 0

        for ((label, score) in labels) {
            if (italianIndicators.any { indicator ->
                    label.lowercase().contains(indicator)
                }) {
                totalScore += score
                matchCount++
            }
        }

        return if (matchCount > 0) totalScore / matchCount else 0f
    }
}