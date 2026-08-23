package com.jesunez.recetairo.feature.recipe.ui

import com.jesunez.recetairo.feature.recipe.domain.model.Recipe

data class GeneratedRecipesUiState(
    val recipes: List<Recipe> = emptyList(),
    val checked: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
)
