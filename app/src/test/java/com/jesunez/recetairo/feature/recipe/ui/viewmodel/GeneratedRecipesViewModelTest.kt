// Feature: recipe-consumer-android, T21*
package com.jesunez.recetairo.feature.recipe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.GeneratedRecipesCache
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeGenerationRepository
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeRepository
import com.jesunez.recetairo.feature.recipe.domain.usecase.GenerateRecipesUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.SaveRecipesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class GeneratedRecipesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRecipeGenerationRepository(
        var result: Result<List<Recipe>>
    ) : RecipeGenerationRepository {
        var callCount = 0
        var lastServings: Int? = null
        override suspend fun generateRecipes(ingredientNames: List<String>, servings: Int): Result<List<Recipe>> {
            callCount++
            lastServings = servings
            return result
        }
    }

    private class FakeRecipeRepository(
        private val saveRecipesResult: Result<Unit> = Result.Success(Unit)
    ) : RecipeRepository {
        var lastSaved: List<Recipe>? = null

        override fun getSavedRecipes(query: String): Flow<Result<List<Recipe>>> =
            throw UnsupportedOperationException()
        override fun getRecipeById(id: Long): Flow<Result<Recipe?>> =
            throw UnsupportedOperationException()
        override suspend fun saveRecipe(recipe: Recipe): Result<Long> =
            throw UnsupportedOperationException()
        override suspend fun saveRecipes(recipes: List<Recipe>): Result<Unit> {
            lastSaved = recipes
            return saveRecipesResult
        }
        override suspend fun removeRecipe(id: Long): Result<Unit> =
            throw UnsupportedOperationException()
    }

    private val generatedRecipes = listOf(
        Recipe(
            title = "Tortilla de Patatas",
            difficulty = RecipeDifficulty.FACIL,
            durationMinutes = 30,
            servings = 4,
            ingredients = listOf(RecipeIngredient(name = "Patata", quantityText = "4 u")),
            steps = listOf("Pelar y cortar las patatas.")
        ),
        Recipe(
            title = "Ensalada César",
            difficulty = RecipeDifficulty.FACIL,
            durationMinutes = 15,
            servings = 2,
            ingredients = listOf(RecipeIngredient(name = "Lechuga", quantityText = "1 u")),
            steps = listOf("Lavar la lechuga.")
        ),
        Recipe(
            title = "Lentejas Estofadas",
            difficulty = RecipeDifficulty.MEDIA,
            durationMinutes = 60,
            servings = 6,
            ingredients = listOf(RecipeIngredient(name = "Lentejas", quantityText = "500 g")),
            steps = listOf("Poner las lentejas en remojo.")
        )
    )

    private fun buildViewModel(
        generationRepository: FakeRecipeGenerationRepository =
            FakeRecipeGenerationRepository(Result.Success(generatedRecipes)),
        recipeRepository: FakeRecipeRepository = FakeRecipeRepository(),
        cache: GeneratedRecipesCache = GeneratedRecipesCache(),
        servings: String? = null
    ): GeneratedRecipesViewModel = GeneratedRecipesViewModel(
        savedStateHandle = SavedStateHandle(
            buildMap {
                put("ingredientNames", "Tomate,Cebolla")
                servings?.let { put("servings", it) }
            }
        ),
        generateRecipesUseCase = GenerateRecipesUseCase(generationRepository),
        saveRecipesUseCase = SaveRecipesUseCase(recipeRepository),
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
    fun should_showErrorWithoutPublishingToCache_when_generationFails() = runTest(testDispatcher) {
        // Given
        val errorMessage = "No se han podido generar recetas. Inténtalo de nuevo."
        val generationRepository = FakeRecipeGenerationRepository(
            Result.Error(RuntimeException("no network"), errorMessage)
        )
        val cache = GeneratedRecipesCache()

        // When
        val viewModel = buildViewModel(generationRepository = generationRepository, cache = cache)
        runCurrent()

        // Then: permanece en la pantalla (sin cerrar/navegar), sin dar ninguna receta por generada
        val state = viewModel.uiState.value
        assertTrue(state.recipes.isEmpty())
        assertEquals(errorMessage, state.error)
        assertFalse(state.isLoading)
        assertTrue(cache.recipes.value.isEmpty())
    }

    @Test
    fun should_retryGeneration_when_retryClickedAfterFailure() = runTest(testDispatcher) {
        // Given: la generación falla al abrir la pantalla
        val generationRepository = FakeRecipeGenerationRepository(
            Result.Error(RuntimeException("no network"), "error")
        )
        val viewModel = buildViewModel(generationRepository = generationRepository)
        runCurrent()
        assertTrue(viewModel.uiState.value.error != null)

        // When: el humano reintenta y esta vez el generador responde con éxito
        generationRepository.result = Result.Success(generatedRecipes)
        viewModel.onRetryClicked()
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(generatedRecipes, state.recipes)
        assertEquals(2, generationRepository.callCount)
    }

    @Test
    fun should_saveOnlyCheckedRecipes_when_saveSelectedClicked() = runTest(testDispatcher) {
        // Given: 3 recetas generadas, se marcan la 1ª y la 3ª
        val recipeRepository = FakeRecipeRepository()
        val viewModel = buildViewModel(recipeRepository = recipeRepository)
        runCurrent()
        viewModel.onRecipeCheckedChanged(0, true)
        viewModel.onRecipeCheckedChanged(2, true)

        // When
        viewModel.onSaveSelectedClicked()
        runCurrent()

        // Then
        assertEquals(listOf(generatedRecipes[0], generatedRecipes[2]), recipeRepository.lastSaved)
        val state = viewModel.uiState.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.error)
    }

    @Test
    fun should_keepScreenWithError_when_saveFails() = runTest(testDispatcher) {
        // Given
        val errorMessage = "No se han podido guardar las recetas seleccionadas."
        val recipeRepository = FakeRecipeRepository(
            saveRecipesResult = Result.Error(RuntimeException("db error"), errorMessage)
        )
        val viewModel = buildViewModel(recipeRepository = recipeRepository)
        runCurrent()
        viewModel.onRecipeCheckedChanged(0, true)

        // When
        viewModel.onSaveSelectedClicked()
        runCurrent()

        // Then: no navega (savedSuccessfully sigue false) y muestra el error
        val state = viewModel.uiState.value
        assertFalse(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun should_passServingsFromRoute_when_generatingRecipes() = runTest(testDispatcher) {
        // Given / When: R4, servings viaja como argumento de la ruta
        val generationRepository = FakeRecipeGenerationRepository(Result.Success(generatedRecipes))
        buildViewModel(generationRepository = generationRepository, servings = "3")
        runCurrent()

        // Then
        assertEquals(3, generationRepository.lastServings)
    }

    @Test
    fun should_defaultServingsToOne_when_routeHasNoServingsArgument() = runTest(testDispatcher) {
        // Given / When: ruta sin "servings" (no debería ocurrir en producción, pero el ViewModel
        // no debe romperse ante su ausencia)
        val generationRepository = FakeRecipeGenerationRepository(Result.Success(generatedRecipes))
        buildViewModel(generationRepository = generationRepository)
        runCurrent()

        // Then
        assertEquals(1, generationRepository.lastServings)
    }

    @Test
    fun should_reuseSameServings_when_retryingAfterFailure() = runTest(testDispatcher) {
        // Given: la generación falla al abrir la pantalla, con servings = 2 elegido en
        // Selector_Ingredientes
        val generationRepository = FakeRecipeGenerationRepository(
            Result.Error(RuntimeException("no network"), "error")
        )
        val viewModel = buildViewModel(generationRepository = generationRepository, servings = "2")
        runCurrent()

        // When: el humano reintenta tras el fallo
        generationRepository.result = Result.Success(generatedRecipes)
        viewModel.onRetryClicked()
        runCurrent()

        // Then: R7, el reintento reutiliza el mismo servings, sin volver a Selector_Ingredientes
        assertEquals(2, generationRepository.callCount)
        assertEquals(2, generationRepository.lastServings)
    }
}
