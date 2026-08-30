package com.jesunez.recetairo.feature.recipe.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeGenerationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateRecipesUseCaseTest {

    private fun buildUseCase(result: Result<List<Recipe>>): GenerateRecipesUseCase {
        val fakeRepository = object : RecipeGenerationRepository {
            override suspend fun generateRecipes(ingredientNames: List<String>, servings: Int): Result<List<Recipe>> =
                result
        }
        return GenerateRecipesUseCase(fakeRepository)
    }

    @Test
    fun should_returnGeneratedRecipes_when_repositorySucceeds() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                title = "Tortilla de patatas",
                difficulty = RecipeDifficulty.FACIL,
                durationMinutes = 30,
                servings = 4,
                ingredients = emptyList(),
                steps = emptyList()
            )
        )
        val useCase = buildUseCase(Result.Success(recipes))

        // When
        val result = useCase(listOf("Patata"), 4)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(recipes, (result as Result.Success).data)
    }

    @Test
    fun should_passThroughError_when_repositoryFails() = runTest {
        // Given
        val exception = RuntimeException("Firebase AI Logic no disponible")
        val useCase = buildUseCase(Result.Error(exception))

        // When
        val result = useCase(listOf("Patata"), 1)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
