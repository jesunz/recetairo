package com.jesunez.recetairo.feature.recipe.domain.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe

interface RecipeGenerationRepository {
    suspend fun generateRecipes(ingredientNames: List<String>): Result<List<Recipe>>
}
