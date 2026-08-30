package com.jesunez.recetairo.feature.recipe.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecipeIngredientDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "quantity") val quantity: String? = null
)
