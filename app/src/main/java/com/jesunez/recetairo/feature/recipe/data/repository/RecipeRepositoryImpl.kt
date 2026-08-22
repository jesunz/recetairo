package com.jesunez.recetairo.feature.recipe.data.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.data.dao.RecipeDao
import com.jesunez.recetairo.feature.recipe.data.entity.RecipeEntity
import com.jesunez.recetairo.feature.recipe.data.mapper.toDomain
import com.jesunez.recetairo.feature.recipe.data.mapper.toEntity
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao
) : RecipeRepository {

    override fun getSavedRecipes(query: String): Flow<Result<List<Recipe>>> {
        val recipes = if (query.isBlank()) recipeDao.getAll() else recipeDao.search(query)
        return recipes
            .map<List<RecipeEntity>, Result<List<Recipe>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .catch { e -> emit(Result.Error(e)) }
    }

    override fun getRecipeById(id: Long): Flow<Result<Recipe?>> =
        recipeDao.getById(id)
            .map<RecipeEntity?, Result<Recipe?>> { entity ->
                Result.Success(entity?.toDomain())
            }
            .catch { e -> emit(Result.Error(e)) }

    override suspend fun saveRecipe(recipe: Recipe): Result<Long> = try {
        Result.Success(recipeDao.insert(recipe.toEntity()))
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun saveRecipes(recipes: List<Recipe>): Result<Unit> = try {
        recipeDao.insertAll(recipes.map { it.toEntity() })
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun removeRecipe(id: Long): Result<Unit> = try {
        recipeDao.deleteById(id)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
