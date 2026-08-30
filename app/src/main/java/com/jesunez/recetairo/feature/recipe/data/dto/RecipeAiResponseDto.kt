package com.jesunez.recetairo.feature.recipe.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecipeAiResponseDto(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "difficulty") val difficulty: String,
    @param:Json(name = "durationMinutes") val durationMinutes: Int,
    @param:Json(name = "servings") val servings: Int,
    @param:Json(name = "ingredients") val ingredients: List<RecipeIngredientDto>,
    @param:Json(name = "steps") val steps: List<String>
)
