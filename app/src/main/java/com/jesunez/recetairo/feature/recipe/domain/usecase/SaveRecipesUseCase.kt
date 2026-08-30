package com.jesunez.recetairo.feature.recipe.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeRepository
import javax.inject.Inject

class SaveRecipesUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(recipes: List<Recipe>): Result<Unit> =
        recipeRepository.saveRecipes(recipes)
}
