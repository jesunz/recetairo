package com.jesunez.recetairo.feature.food.data.mapper

import com.jesunez.recetairo.feature.food.data.entity.FoodEntity
import com.jesunez.recetairo.feature.food.domain.model.Food
import java.time.Instant
import java.time.LocalDate

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

fun FoodEntity.toDomain(): Food = Food(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = category,
    expiryDate = expiryDate?.let { LocalDate.parse(it) },
    imageUrl = imageUrl,
    insertedAt = Instant.ofEpochMilli(insertedAt)
)
