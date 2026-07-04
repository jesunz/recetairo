package com.jesunez.recetairo.feature.food.domain.model

enum class FoodCategory {
    LACTEOS,
    CARNE,
    PESCADO,
    MARISCO,
    FRUTAS,
    VERDURAS,
    PAN,
    CEREALES,
    OTROS;

    fun label(): String = when (this) {
        LACTEOS -> "Lácteos"
        CARNE -> "Carne"
        PESCADO -> "Pescado"
        MARISCO -> "Marisco"
        FRUTAS -> "Frutas"
        VERDURAS -> "Verduras"
        PAN -> "Pan"
        CEREALES -> "Cereales"
        OTROS -> "Otros"
    }

    companion object {
        fun fromLabel(raw: String): FoodCategory =
            entries.firstOrNull { it.label().equals(raw, ignoreCase = true) } ?: OTROS
    }
}
