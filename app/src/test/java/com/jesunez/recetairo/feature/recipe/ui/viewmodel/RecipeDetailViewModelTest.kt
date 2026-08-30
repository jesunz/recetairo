// Feature: recipe-consumer-android, T26*
package com.jesunez.recetairo.feature.recipe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.recipe.domain.GeneratedRecipesCache
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeRepository
import com.jesunez.recetairo.feature.recipe.domain.usecase.ComputePantryMatchUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.GetRecipeByIdUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.RemoveRecipeUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.SaveRecipeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRecipeRepository(
        private val recipeByIdResult: Result<Recipe?> = Result.Success(null),
        private val saveResult: Result<Long> = Result.Success(1L),
        private val removeResult: Result<Unit> = Result.Success(Unit)
    ) : RecipeRepository {
        var lastRemovedId: Long? = null
        var lastSaved: Recipe? = null

        override fun getSavedRecipes(query: String): Flow<Result<List<Recipe>>> =
            throw UnsupportedOperationException()
        override fun getRecipeById(id: Long): Flow<Result<Recipe?>> = flowOf(recipeByIdResult)
        override suspend fun saveRecipe(recipe: Recipe): Result<Long> {
            lastSaved = recipe
            return saveResult
        }
        override suspend fun saveRecipes(recipes: List<Recipe>): Result<Unit> =
            throw UnsupportedOperationException()
        override suspend fun removeRecipe(id: Long): Result<Unit> {
            lastRemovedId = id
            return removeResult
        }
    }

    private class FakeFoodRepository(
        private val pantryResult: Result<List<Food>> = Result.Success(emptyList())
    ) : FoodRepository {
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

    private val recipe = Recipe(
        id = 7L,
        title = "Tortilla de Patatas",
        difficulty = RecipeDifficulty.FACIL,
        durationMinutes = 30,
        servings = 4,
        ingredients = listOf(
            RecipeIngredient(name = "Patata", quantityText = "4 u"),
            RecipeIngredient(name = "Huevo", quantityText = "6 u")
        ),
        steps = listOf("Pelar y cortar las patatas.", "Batir los huevos.")
    )

    private fun buildViewModel(
        arguments: Map<String, String> = mapOf("recipeId" to "7"),
        recipeRepository: FakeRecipeRepository = FakeRecipeRepository(
            recipeByIdResult = Result.Success(recipe)
        ),
        foodRepository: FakeFoodRepository = FakeFoodRepository(),
        cache: GeneratedRecipesCache = GeneratedRecipesCache()
    ): RecipeDetailViewModel = RecipeDetailViewModel(
        savedStateHandle = SavedStateHandle(arguments),
        getRecipeByIdUseCase = GetRecipeByIdUseCase(recipeRepository),
        computePantryMatchUseCase = ComputePantryMatchUseCase(foodRepository),
        saveRecipeUseCase = SaveRecipeUseCase(recipeRepository),
        removeRecipeUseCase = RemoveRecipeUseCase(recipeRepository),
        generatedRecipesCache = cache
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun should_computeMatchPercentage_when_savedRecipeLoadedById() = runTest(testDispatcher) {
        // Given: solo "Patata" está en la despensa (1/2 ingredientes)
        val foodRepository = FakeFoodRepository(Result.Success(listOf(Food(name = "Patata", quantity = 3.0))))
        val recipeRepository = FakeRecipeRepository(recipeByIdResult = Result.Success(recipe))

        // When
        val viewModel = buildViewModel(recipeRepository = recipeRepository, foodRepository = foodRepository)
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(recipe, state.recipe)
        assertTrue(state.isSaved)
        assertFalse(state.notFound)
        assertEquals(50, state.match?.percentage)
        val owned = state.match?.ingredients?.filter { it.owned }?.map { it.ingredient.name }
        assertEquals(listOf("Patata"), owned)
    }

    @Test
    fun should_computeMatchAndMarkUnsaved_when_generatedRecipeLoadedByIndexFromCache() = runTest(testDispatcher) {
        // Given: receta recién generada (aún no persistida), publicada en el caché por GeneratedRecipesViewModel
        val cache = GeneratedRecipesCache()
        cache.publish(listOf(recipe))

        // When
        val viewModel = buildViewModel(arguments = mapOf("generatedIndex" to "0"), cache = cache)
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(recipe, state.recipe)
        assertFalse(state.isSaved)
        assertFalse(state.notFound)
        assertEquals(0, state.match?.percentage)
    }

    @Test
    fun should_markNotFound_when_recipeIdDoesNotExistInRoom() = runTest(testDispatcher) {
        // Given
        val recipeRepository = FakeRecipeRepository(recipeByIdResult = Result.Success(null))

        // When
        val viewModel = buildViewModel(recipeRepository = recipeRepository)
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.notFound)
        assertNull(state.recipe)
        assertFalse(state.isLoading)
    }

    @Test
    fun should_markNotFound_when_neitherRecipeIdNorGeneratedIndexProvided() = runTest(testDispatcher) {
        // When
        val viewModel = buildViewModel(arguments = emptyMap())
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.notFound)
        assertNull(state.recipe)
        assertFalse(state.isLoading)
    }

    @Test
    fun should_removeFromRoomAndUpdateIconWithoutNavigating_when_favoriteClickedOnSavedRecipe() = runTest(testDispatcher) {
        // Given
        val recipeRepository = FakeRecipeRepository(recipeByIdResult = Result.Success(recipe))
        val viewModel = buildViewModel(recipeRepository = recipeRepository)
        runCurrent()
        assertTrue(viewModel.uiState.value.isSaved)

        // When
        viewModel.onFavoriteClicked()
        runCurrent()

        // Then: se elimina de Room, el icono cambia y la Receta sigue visible (sin navegar)
        val state = viewModel.uiState.value
        assertEquals(7L, recipeRepository.lastRemovedId)
        assertFalse(state.isSaved)
        assertFalse(state.notFound)
        assertEquals(0L, state.recipe?.id)
        assertNull(state.error)
    }

    @Test
    fun should_saveToRoomAndUpdateIconWithoutNavigating_when_favoriteClickedOnGeneratedRecipe() = runTest(testDispatcher) {
        // Given
        val cache = GeneratedRecipesCache()
        cache.publish(listOf(recipe))
        val recipeRepository = FakeRecipeRepository(saveResult = Result.Success(42L))
        val viewModel = buildViewModel(
            arguments = mapOf("generatedIndex" to "0"),
            recipeRepository = recipeRepository,
            cache = cache
        )
        runCurrent()
        assertFalse(viewModel.uiState.value.isSaved)

        // When
        viewModel.onFavoriteClicked()
        runCurrent()

        // Then: se guarda en Room con el id asignado y el icono cambia, sin navegar fuera de la pantalla
        val state = viewModel.uiState.value
        assertEquals(recipe, recipeRepository.lastSaved)
        assertTrue(state.isSaved)
        assertFalse(state.notFound)
        assertEquals(42L, state.recipe?.id)
        assertNull(state.error)
    }

    @Test
    fun should_setErrorWithoutChangingIsSaved_when_removeFails() = runTest(testDispatcher) {
        // Given
        val recipeRepository = FakeRecipeRepository(
            recipeByIdResult = Result.Success(recipe),
            removeResult = Result.Error(RuntimeException("db error"))
        )
        val viewModel = buildViewModel(recipeRepository = recipeRepository)
        runCurrent()

        // When
        viewModel.onFavoriteClicked()
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertEquals("No se ha podido quitar la receta de guardadas.", state.error)
    }

    @Test
    fun should_setErrorWithoutChangingIsSaved_when_saveFails() = runTest(testDispatcher) {
        // Given
        val cache = GeneratedRecipesCache()
        cache.publish(listOf(recipe))
        val recipeRepository = FakeRecipeRepository(
            saveResult = Result.Error(RuntimeException("db error"))
        )
        val viewModel = buildViewModel(
            arguments = mapOf("generatedIndex" to "0"),
            recipeRepository = recipeRepository,
            cache = cache
        )
        runCurrent()

        // When
        viewModel.onFavoriteClicked()
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertEquals("No se ha podido guardar la receta.", state.error)
    }

    @Test
    fun should_generateExpectedShareText_when_recipeLoaded() = runTest(testDispatcher) {
        // Given
        val recipeRepository = FakeRecipeRepository(recipeByIdResult = Result.Success(recipe))

        // When
        val viewModel = buildViewModel(recipeRepository = recipeRepository)
        runCurrent()

        // Then
        val expected = buildString {
            appendLine("Tortilla de Patatas")
            appendLine()
            appendLine("Ingredientes:")
            appendLine("- Patata (4 u)")
            appendLine("- Huevo (6 u)")
            appendLine()
            appendLine("Pasos:")
            appendLine("1. Pelar y cortar las patatas.")
            appendLine("2. Batir los huevos.")
        }.trim()
        assertEquals(expected, viewModel.uiState.value.shareText)
    }
}
