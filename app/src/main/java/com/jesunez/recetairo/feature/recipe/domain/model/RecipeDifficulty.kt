package com.jesunez.recetairo.feature.recipe.domain.model

enum class RecipeDifficulty {
    FACIL,
    MEDIA,
    DIFICIL;

    fun label(): String = when (this) {
        FACIL -> "Fácil"
        MEDIA -> "Media"
        DIFICIL -> "Difícil"
    }

    companion object {
        fun fromLabel(raw: String): RecipeDifficulty =
            entries.firstOrNull { it.label().equals(raw, ignoreCase = true) } ?: FACIL
    }
}
