package com.example.madeinitaly.data

data class ProductDataModel(
    val id: String = "",
    val name: String = "",
    val manufacturer: String = "",
    val certifications: List<String> = emptyList(),
    val productionDate: String = "",
    val serialNumber: String = "",
    val productionLocation: String = "",
    val authenticityCode: String = "",
    val confidenceScore: Float = 0.0f
) {
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                (manufacturer.isNotBlank() || authenticityCode.isNotBlank())
    }

    fun getAuthenticityConfidence(): Float {
        // Basic confidence calculation based on available data completeness
        var score = 0.0f
        if (name.isNotBlank()) score += 0.1f
        if (manufacturer.isNotBlank()) score += 0.1f
        if (certifications.isNotEmpty()) score += 0.2f
        if (productionDate.isNotBlank()) score += 0.1f
        if (serialNumber.isNotBlank()) score += 0.2f
        if (productionLocation.isNotBlank()) score += 0.1f
        if (authenticityCode.isNotBlank()) score += 0.2f

        return score + confidenceScore * 0.2f
    }
}