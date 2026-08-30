package com.jesunez.recetairo.feature.recipe.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeGenerationRepository
import javax.inject.Inject

class GenerateRecipesUseCase @Inject constructor(
    private val recipeGenerationRepository: RecipeGenerationRepository
) {
    suspend operator fun invoke(ingredientNames: List<String>): Result<List<Recipe>> =
        recipeGenerationRepository.generateRecipes(ingredientNames)
}
