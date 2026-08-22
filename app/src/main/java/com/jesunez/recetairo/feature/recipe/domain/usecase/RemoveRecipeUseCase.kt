package com.jesunez.recetairo.feature.recipe.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeRepository
import javax.inject.Inject

class RemoveRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = recipeRepository.removeRecipe(id)
}
