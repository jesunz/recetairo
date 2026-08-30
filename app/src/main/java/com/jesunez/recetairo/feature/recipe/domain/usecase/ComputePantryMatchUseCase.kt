package com.jesunez.recetairo.feature.recipe.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredientMatch
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeMatch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ComputePantryMatchUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    suspend operator fun invoke(recipe: Recipe): RecipeMatch {
        val pantryNames = when (val result = foodRepository.getAllFoods().first()) {
            is Result.Success -> result.data.map { it.name }
            else -> emptyList()
        }

        val matches = recipe.ingredients.map { ingredient ->
            val owned = pantryNames.any { pantryName ->
                pantryName.contains(ingredient.name, ignoreCase = true) ||
                    ingredient.name.contains(pantryName, ignoreCase = true)
            }
            RecipeIngredientMatch(ingredient = ingredient, owned = owned)
        }

        val percentage = if (matches.isEmpty()) {
            0
        } else {
            (matches.count { it.owned } * 100) / matches.size
        }

        return RecipeMatch(percentage = percentage, ingredients = matches)
    }
}
