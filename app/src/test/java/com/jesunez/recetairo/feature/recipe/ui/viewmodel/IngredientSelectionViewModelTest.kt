// Feature: recipe-consumer-android, T21*
package com.jesunez.recetairo.feature.recipe.ui.viewmodel

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.usecase.GetAllFoodsUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IngredientSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeFoodRepository(
        private val allFoods: Flow<Result<List<Food>>>
    ) : FoodRepository {
        override suspend fun insertFood(food: Food): Result<Unit> =
            throw UnsupportedOperationException()
        override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
            throw UnsupportedOperationException()
        override fun searchFoodNames(query: String): Flow<List<String>> =
            throw UnsupportedOperationException()
        override fun getAllFoods(): Flow<Result<List<Food>>> = allFoods
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

    private val pantryFoods = listOf(
        Food(id = 1, name = "Tomate", quantity = 3.0, category = FoodCategory.VERDURAS.label()),
        Food(id = 2, name = "Cebolla", quantity = 2.0, category = FoodCategory.VERDURAS.label()),
        Food(id = 3, name = "Pimiento", quantity = 1.0, category = FoodCategory.VERDURAS.label()),
        Food(id = 4, name = "Leche", quantity = 1.0, category = FoodCategory.LACTEOS.label())
    )

    private fun buildViewModel(
        foods: List<Food> = pantryFoods
    ): IngredientSelectionViewModel = IngredientSelectionViewModel(
        getAllFoodsUseCase = GetAllFoodsUseCase(FakeFoodRepository(flowOf(Result.Success(foods))))
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
    fun should_rejectFourthSelection_when_threeIngredientsAlreadySelected() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()
        viewModel.onIngredientToggled("Tomate")
        viewModel.onIngredientToggled("Cebolla")
        viewModel.onIngredientToggled("Pimiento")

        // When: un cuarto ingrediente, con los 3 ya elegidos
        viewModel.onIngredientToggled("Leche")

        // Then: el 4º se rechaza sin tocar los 3 ya seleccionados
        val selected = viewModel.uiState.value.selectedNames
        assertEquals(setOf("Tomate", "Cebolla", "Pimiento"), selected)
        assertFalse("Leche" in selected)
    }

    @Test
    fun should_allowNewSelection_when_deselectingBelowLimitFirst() = runTest(testDispatcher) {
        // Given: 3 ingredientes seleccionados (límite alcanzado)
        val viewModel = buildViewModel()
        runCurrent()
        viewModel.onIngredientToggled("Tomate")
        viewModel.onIngredientToggled("Cebolla")
        viewModel.onIngredientToggled("Pimiento")

        // When: se deselecciona uno y se elige otro distinto
        viewModel.onIngredientToggled("Pimiento")
        viewModel.onIngredientToggled("Leche")

        // Then
        assertEquals(setOf("Tomate", "Cebolla", "Leche"), viewModel.uiState.value.selectedNames)
    }

    @Test
    fun should_reflectSelectionCount_when_ingredientsToggledOneByOne() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()

        // When / Then
        viewModel.onIngredientToggled("Tomate")
        assertEquals(1, viewModel.uiState.value.selectedNames.size)

        viewModel.onIngredientToggled("Cebolla")
        assertEquals(2, viewModel.uiState.value.selectedNames.size)

        // Volver a tocar un ingrediente ya seleccionado lo quita
        viewModel.onIngredientToggled("Tomate")
        assertEquals(1, viewModel.uiState.value.selectedNames.size)
    }

    @Test
    fun should_disableGenerateButton_when_noIngredientSelected() = runTest(testDispatcher) {
        // Given / When
        val viewModel = buildViewModel()
        runCurrent()

        // Then
        assertFalse(viewModel.uiState.value.isGenerateEnabled)
    }

    @Test
    fun should_enableGenerateButton_when_atLeastOneIngredientSelected() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()

        // When
        viewModel.onIngredientToggled("Tomate")

        // Then
        assertTrue(viewModel.uiState.value.isGenerateEnabled)
    }

    @Test
    fun should_disableGenerateButtonAgain_when_lastSelectionRemoved() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()
        viewModel.onIngredientToggled("Tomate")

        // When
        viewModel.onIngredientToggled("Tomate")

        // Then
        assertFalse(viewModel.uiState.value.isGenerateEnabled)
    }

    @Test
    fun should_defaultToOneServing_when_screenOpens() = runTest(testDispatcher) {
        // Given / When
        val viewModel = buildViewModel()
        runCurrent()

        // Then: R1
        assertEquals(1, viewModel.uiState.value.servings)
    }

    @Test
    fun should_updateServingsWithoutTouchingSelection_when_servingsSelected() = runTest(testDispatcher) {
        // Given: 2 ingredientes ya seleccionados
        val viewModel = buildViewModel()
        runCurrent()
        viewModel.onIngredientToggled("Tomate")
        viewModel.onIngredientToggled("Cebolla")

        // When
        viewModel.onServingsSelected(4)

        // Then: R3, la selección de alimentos no cambia
        val state = viewModel.uiState.value
        assertEquals(4, state.servings)
        assertEquals(setOf("Tomate", "Cebolla"), state.selectedNames)
    }

    @Test
    fun should_clampServingsToMax_when_valueAboveFour() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()

        // When
        viewModel.onServingsSelected(7)

        // Then
        assertEquals(4, viewModel.uiState.value.servings)
    }

    @Test
    fun should_clampServingsToMin_when_valueBelowOne() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()

        // When
        viewModel.onServingsSelected(0)

        // Then
        assertEquals(1, viewModel.uiState.value.servings)
    }
}
