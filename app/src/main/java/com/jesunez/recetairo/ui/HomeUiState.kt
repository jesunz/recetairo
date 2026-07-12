package com.jesunez.recetairo.ui

import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food

data class HomeUiState(
    val categorySummaries: List<CategorySummary> = emptyList(),
    val expiringSoonFoods: List<Food> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
