package com.jesunez.recetairo.feature.recipe.domain.model

data class RecipeMatch(
    val percentage: Int,
    val ingredients: List<RecipeIngredientMatch>
)
