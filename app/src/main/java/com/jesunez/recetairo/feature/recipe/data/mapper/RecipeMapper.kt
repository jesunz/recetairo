package com.jesunez.recetairo.feature.recipe.data.mapper

import com.jesunez.recetairo.feature.recipe.data.dto.RecipeAiResponseDto
import com.jesunez.recetairo.feature.recipe.data.dto.RecipeIngredientDto
import com.jesunez.recetairo.feature.recipe.data.entity.RecipeEntity
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import java.time.Instant

fun RecipeEntity.toDomain(): Recipe = Recipe(
    id = id,
    title = title,
    difficulty = RecipeDifficulty.fromLabel(difficulty),
    durationMinutes = durationMinutes,
    servings = servings,
    ingredients = ingredientsJson,
    steps = stepsJson,
    savedAt = Instant.ofEpochMilli(savedAt)
)

fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    id = id,
    title = title,
    difficulty = difficulty.label(),
    durationMinutes = durationMinutes,
    servings = servings,
    ingredientsJson = ingredients,
    stepsJson = steps,
    savedAt = (savedAt ?: Instant.now()).toEpochMilli()
)

// Gemini occasionally appends a stray trailing "+" to an ingredient's "name" field
// (e.g. "Patatas+") even though the response schema only declares it as a plain string.
// Strip it here so the displayed name and the pantry-match comparison both use the clean name.
private val TRAILING_PLUS_REGEX = Regex("\\s*\\+\\s*$")

fun RecipeIngredientDto.toDomain(): RecipeIngredient = RecipeIngredient(
    name = name.replace(TRAILING_PLUS_REGEX, "").trim(),
    quantityText = quantity ?: ""
)

fun RecipeAiResponseDto.toDomain(): Recipe = Recipe(
    title = title,
    difficulty = RecipeDifficulty.fromLabel(difficulty),
    durationMinutes = durationMinutes,
    servings = servings,
    ingredients = ingredients.map { it.toDomain() },
    steps = steps,
    savedAt = null
)

fun List<RecipeAiResponseDto>.toDomain(): List<Recipe> = map { it.toDomain() }
