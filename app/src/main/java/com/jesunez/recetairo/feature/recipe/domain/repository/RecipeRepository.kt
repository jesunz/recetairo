package com.jesunez.recetairo.feature.recipe.domain.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun getSavedRecipes(query: String = ""): Flow<Result<List<Recipe>>>
    fun getRecipeById(id: Long): Flow<Result<Recipe?>>
    suspend fun saveRecipe(recipe: Recipe): Result<Long>
    suspend fun saveRecipes(recipes: List<Recipe>): Result<Unit>
    suspend fun removeRecipe(id: Long): Result<Unit>
}
