package com.jesunez.recetairo.ui.component

import com.jesunez.recetairo.feature.food.domain.model.FoodCategory

internal fun FoodCategory.emoji(): String = when (this) {
    FoodCategory.LACTEOS -> "🧀"
    FoodCategory.CARNE -> "🥩"
    FoodCategory.PESCADO -> "🐟"
    FoodCategory.MARISCO -> "🦐"
    FoodCategory.FRUTAS -> "🍎"
    FoodCategory.VERDURAS -> "🥦"
    FoodCategory.PAN -> "🍞"
    FoodCategory.CEREALES -> "🌾"
    FoodCategory.OTROS -> "📦"
}
