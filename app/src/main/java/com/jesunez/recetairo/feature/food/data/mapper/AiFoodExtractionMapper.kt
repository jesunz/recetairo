package com.jesunez.recetairo.feature.food.data.mapper

import com.jesunez.recetairo.feature.food.data.dto.AiFoodItemDto
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem

fun AiFoodItemDto.toDomain(): OcrFoodItem = OcrFoodItem(
    name = name,
    quantity = quantity ?: "",
    expiryDate = "",
    category = FoodCategory.fromLabel(category),
    confidence = 1.0f,
    isVerified = true,
    needsReview = needsReview
)

fun List<AiFoodItemDto>.toDomain(): List<OcrFoodItem> = map { it.toDomain() }
