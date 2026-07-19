package com.jesunez.recetairo.feature.food.ui

import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.PantryFilter

data class PantryUiState(
    val filter: PantryFilter = PantryFilter.All,
    val foods: List<Food> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
