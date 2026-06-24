package com.jesunez.recetairo.feature.food.domain.model

data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<FoodField, String> = emptyMap()
)
