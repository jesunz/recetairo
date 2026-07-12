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
        // "x"/"X" are allowed as multiplication separators (e.g. "2x3"), but that same
        // char class also matches food names made up entirely of x/X letters (e.g. "Xxxx").
        // Requiring at least one digit keeps that false positive out while still catching
        // every realistic numeric-noise line (quantities, prices, phone fragments).
        if (trimmed.any(Char::isDigit) && NUMERIC_ONLY_REGEX.matches(trimmed)) return true
        val normalized = trimmed.uppercase()
            .replace(Regex("[^A-ZÁÉÍÓÚÑ ]"), "")
            .trim()
        return normalized in NON_FOOD_TOKENS
    }
}
