package com.jesunez.recetairo.feature.food.domain.model

data class OcrFoodItem(
    val name: String,
    val quantity: String,
    val expiryDate: String,
    val category: FoodCategory = FoodCategory.OTROS,
    val confidence: Float,
    val isVerified: Boolean,
    val isSelected: Boolean = true,
    val needsReview: Boolean = false,
    val unit: FoodUnit = FoodUnit.UNIDADES,
    val emoji: String? = null
)
