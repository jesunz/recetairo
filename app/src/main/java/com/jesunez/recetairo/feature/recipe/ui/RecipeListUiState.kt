package com.jesunez.recetairo.feature.recipe.ui

import com.jesunez.recetairo.feature.recipe.domain.model.Recipe

data class RecipeListUiState(
    val query: String = "",
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
