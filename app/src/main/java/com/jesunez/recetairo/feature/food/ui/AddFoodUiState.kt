package com.jesunez.recetairo.feature.food.ui

import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.FoodField
import com.jesunez.recetairo.feature.food.domain.model.FoodUnit
import com.jesunez.recetairo.feature.food.domain.model.SaveResult

data class AddFoodUiState(
    val name: String = "",
    val quantity: String = "",
    val unit: String = FoodUnit.UNIDADES.label(),
    val category: String = FoodCategory.OTROS.label(),
    val brand: String = "",
    val expiryDate: String = "",
    val imageUrl: String? = null,
    val nameSuggestions: List<String> = emptyList(),
    val validationErrors: Map<FoodField, String> = emptyMap(),
    val saveResult: SaveResult? = null,
    val isLoading: Boolean = false
)
