package com.jesunez.recetairo.feature.food.ui

import com.jesunez.recetairo.feature.food.domain.model.FoodField
import com.jesunez.recetairo.feature.food.domain.model.SaveResult

data class AddFoodUiState(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    val category: String = "",
    val brand: String = "",
    val expiryDate: String = "",
    val imageUrl: String? = null,
    val nameSuggestions: List<String> = emptyList(),
    val validationErrors: Map<FoodField, String> = emptyMap(),
    val saveResult: SaveResult? = null,
    val isLoading: Boolean = false
)
