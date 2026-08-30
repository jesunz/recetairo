package com.jesunez.recetairo.feature.recipe.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputePantryMatchUseCaseTest {

    private fun buildUseCase(pantryResult: Result<List<Food>>): ComputePantryMatchUseCase {
        val fakeRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun searchFoodNames(query: String): Flow<List<String>> =
                throw UnsupportedOperationException()
            override fun getAllFoods(): Flow<Result<List<Food>>> = flowOf(pantryResult)
            override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
            override suspend fun deleteFood(foodId: Long): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun getFoodById(foodId: Long): Flow<Result<Food?>> =
                throw UnsupportedOperationException()
        }
        return ComputePantryMatchUseCase(fakeRepository)
    }

    private fun buildRecipe(ingredientNames: List<String>) = Recipe(
        title = "Receta de prueba",
        difficulty = RecipeDifficulty.FACIL,
        durationMinutes = 20,
        servings = 2,
        ingredients = ingredientNames.map { RecipeIngredient(name = it, quantityText = "1 unidad") },
        steps = listOf("Paso 1")
    )

    @Test
    fun should_calculateCorrectPercentage_when_someIngredientsAreOwned() = runTest {
        // Given
        val recipe = buildRecipe(listOf("Tomate", "Cebolla", "Ajo", "Pimiento"))
        val pantry = listOf(
            Food(name = "Tomate", quantity = 3.0),
            Food(name = "Cebolla", quantity = 2.0)
        )
        val useCase = buildUseCase(Result.Success(pantry))

        // When
        val match = useCase(recipe)

        // Then
        assertEquals(50, match.percentage)
    }

    @Test
    fun should_distinguishOwnedFromMissingIngredients_when_computingMatch() = runTest {
        // Given
        val recipe = buildRecipe(listOf("Tomate", "Cebolla", "Ajo"))
        val pantry = listOf(Food(name = "Tomate", quantity = 3.0))
        val useCase = buildUseCase(Result.Success(pantry))

        // When
        val match = useCase(recipe)

        // Then
        val owned = match.ingredients.filter { it.owned }.map { it.ingredient.name }
        val missing = match.ingredients.filter { !it.owned }.map { it.ingredient.name }
        assertEquals(listOf("Tomate"), owned)
        assertEquals(listOf("Cebolla", "Ajo"), missing)
    }

    @Test
    fun should_returnZeroPercentAndAllMissing_when_pantryIsEmpty() = runTest {
        // Given
        val recipe = buildRecipe(listOf("Tomate", "Cebolla"))
        val useCase = buildUseCase(Result.Success(emptyList()))

        // When
        val match = useCase(recipe)

        // Then
        assertEquals(0, match.percentage)
        assertTrue(match.ingredients.none { it.owned })
    }

    @Test
    fun should_treatPantryAsEmpty_when_repositoryReturnsError() = runTest {
        // Given
        val recipe = buildRecipe(listOf("Tomate"))
        val useCase = buildUseCase(Result.Error(RuntimeException("db failure")))

        // When
        val match = useCase(recipe)

        // Then
        assertEquals(0, match.percentage)
        assertFalse(match.ingredients.first().owned)
    }

    @Test
    fun should_matchCaseInsensitivelyAndPartially_when_pantryNameContainsIngredientName() = runTest {
        // Given
        val recipe = buildRecipe(listOf("tomate"))
        val pantry = listOf(Food(name = "Tomates cherry", quantity = 1.0))
        val useCase = buildUseCase(Result.Success(pantry))

        // When
        val match = useCase(recipe)

        // Then
        assertTrue(match.ingredients.first().owned)
    }
}
