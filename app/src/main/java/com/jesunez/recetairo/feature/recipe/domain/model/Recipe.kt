package com.jesunez.recetairo.feature.recipe.domain.model

import java.time.Instant

data class Recipe(
    val id: Long = 0,
    val title: String,
    val difficulty: RecipeDifficulty,
    val durationMinutes: Int,
    val servings: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val savedAt: Instant? = null
)
