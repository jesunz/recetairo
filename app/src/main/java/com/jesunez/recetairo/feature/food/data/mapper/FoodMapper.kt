package com.jesunez.recetairo.feature.food.data.mapper

import com.jesunez.recetairo.feature.food.data.entity.FoodEntity
import com.jesunez.recetairo.feature.food.domain.model.Food

fun Food.toEntity(): FoodEntity = FoodEntity(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = category,
    expiryDate = expiryDate?.toString(),
    imageUrl = imageUrl,
    insertedAt = insertedAt.toEpochMilli()
)
