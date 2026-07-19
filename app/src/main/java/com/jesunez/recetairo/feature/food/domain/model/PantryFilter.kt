package com.jesunez.recetairo.feature.food.domain.model

sealed class PantryFilter {
    data object All : PantryFilter()
    data class ByCategory(val category: FoodCategory) : PantryFilter()
    data object ExpiringSoon : PantryFilter()
}
