package com.jesunez.recetairo.feature.food.domain.model

enum class FoodUnit {
    UNIDADES,
    GRAMOS,
    KILOGRAMOS,
    MILILITROS,
    LITROS,
    LATAS,
    PAQUETES,
    DOCENA,
    BARRAS,
    OTROS;

    fun label(): String = when (this) {
        UNIDADES -> "unidades"
        GRAMOS -> "gramos"
        KILOGRAMOS -> "kilogramos"
        MILILITROS -> "mililitros"
        LITROS -> "litros"
        LATAS -> "latas"
        PAQUETES -> "paquetes"
        DOCENA -> "docena"
        BARRAS -> "barras"
        OTROS -> "otros"
    }

    companion object {
        fun fromLabel(raw: String): FoodUnit =
            entries.firstOrNull { it.label().equals(raw, ignoreCase = true) } ?: UNIDADES
    }
}
