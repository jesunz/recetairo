package com.jesunez.recetairo.feature.recipe.ui

import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory

data class IngredientSelectionUiState(
    val categorized: Map<FoodCategory, List<Food>> = emptyMap(),
    val expandedCategory: FoodCategory? = null,
    val selectedNames: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    // R9: "Generar Receta" stays disabled while no ingredient is selected
    val isGenerateEnabled: Boolean get() = selectedNames.isNotEmpty()
}
