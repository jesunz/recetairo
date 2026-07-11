package com.jesunez.recetairo.feature.food.domain.util

object NumericNoiseFilter {

    private val NUMERIC_ONLY_REGEX = Regex("^[\\d\\s.,€\$xX/-]+\$")

    private val NON_FOOD_TOKENS = setOf(
        "TEL", "TELEFONO", "CIF", "NIF", "IVA", "TOTAL", "SUBTOTAL",
        "CAMBIO", "TARJETA", "EFECTIVO", "TICKET", "FACTURA"
    )

    fun isNoise(rawLine: String): Boolean {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return true
        if (NUMERIC_ONLY_REGEX.matches(trimmed)) return true
        val normalized = trimmed.uppercase()
            .replace(Regex("[^A-ZÁÉÍÓÚÑ ]"), "")
            .trim()
        return normalized in NON_FOOD_TOKENS
    }
}
