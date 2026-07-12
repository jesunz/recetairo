package com.jesunez.recetairo.feature.food.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiFoodItemDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "quantity") val quantity: String? = null,
    @param:Json(name = "category") val category: String,
    @param:Json(name = "needsReview") val needsReview: Boolean = false
)
