package com.jesunez.recetairo.feature.food.ui

import com.jesunez.recetairo.feature.food.domain.model.Food

data class FoodDetailUiState(
    val food: Food? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val navigateBack: Boolean = false
)
