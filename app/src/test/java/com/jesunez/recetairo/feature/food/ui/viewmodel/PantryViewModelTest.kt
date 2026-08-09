// Feature: consultar-despensa, T14
package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.PantryFilter
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.usecase.GetAllFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetExpiringSoonFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetFoodsByCategoryUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeFoodRepository(
        private val allFoods: Flow<Result<List<Food>>> = flowOf(Result.Success(emptyList())),
        private val foodsByCategory: Flow<Result<List<Food>>> = flowOf(Result.Success(emptyList())),
        private val expiringSoonFoods: Flow<Result<List<Food>>> = flowOf(Result.Success(emptyList()))
    ) : FoodRepository {
        var lastCategoryRequested: FoodCategory? = null

        override suspend fun insertFood(food: Food): Result<Unit> =
            throw UnsupportedOperationException()
        override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
            throw UnsupportedOperationException()
        override fun searchFoodNames(query: String): Flow<List<String>> =
            throw UnsupportedOperationException()
        override fun getAllFoods(): Flow<Result<List<Food>>> = allFoods
        override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> {
            lastCategoryRequested = category
            return foodsByCategory
        }
        override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> = expiringSoonFoods
        override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
            throw UnsupportedOperationException()
        override suspend fun deleteFood(foodId: Long): Result<Unit> =
            throw UnsupportedOperationException()
        override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> =
            throw UnsupportedOperationException()
        override fun getFoodById(foodId: Long): Flow<Result<Food?>> =
            throw UnsupportedOperationException()
    }

    private fun buildViewModel(
        savedStateHandle: SavedStateHandle,
        fakeRepository: FakeFoodRepository = FakeFoodRepository()
    ): PantryViewModel = PantryViewModel(
        savedStateHandle = savedStateHandle,
        getAllFoodsUseCase = GetAllFoodsUseCase(fakeRepository),
        getFoodsByCategoryUseCase = GetFoodsByCategoryUseCase(fakeRepository),
        getExpiringSoonFoodsUseCase = GetExpiringSoonFoodsUseCase(fakeRepository)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region resolución del filtro (R2, R5, R6)

    @Test
    fun should_useAllFilter_when_noRouteArgsProvided() = runTest(testDispatcher) {
        // Given / When
        val viewModel = buildViewModel(SavedStateHandle())
        runCurrent()

        // Then
        assertEquals(PantryFilter.All, viewModel.uiState.value.filter)
    }

    @Test
    fun should_useByCategoryFilter_when_categoryArgProvided() = runTest(testDispatcher) {
        // Given
        val fakeRepository = FakeFoodRepository()
        val savedStateHandle = SavedStateHandle(mapOf("category" to "LACTEOS"))

        // When
        val viewModel = buildViewModel(savedStateHandle, fakeRepository)
        runCurrent()

        // Then
        assertEquals(PantryFilter.ByCategory(FoodCategory.LACTEOS), viewModel.uiState.value.filter)
        assertEquals(FoodCategory.LACTEOS, fakeRepository.lastCategoryRequested)
    }

    @Test
    fun should_useExpiringSoonFilter_when_expiringSoonArgIsTrue() = runTest(testDispatcher) {
        // Given
        val savedStateHandle = SavedStateHandle(mapOf("expiringSoon" to "true"))

        // When
        val viewModel = buildViewModel(savedStateHandle)
        runCurrent()

        // Then
        assertEquals(PantryFilter.ExpiringSoon, viewModel.uiState.value.filter)
    }

    @Test
    fun should_showLoading_when_viewModelIsInitialized() {
        // Given / When — el dispatcher aún no ha avanzado
        val viewModel = buildViewModel(SavedStateHandle())

        // Then
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun should_setError_when_queryFails() = runTest(testDispatcher) {
        // Given
        val errorMessage = "No se ha podido cargar la despensa."
        val fakeRepository = FakeFoodRepository(
            allFoods = flowOf(Result.Error(RuntimeException("db error"), errorMessage))
        )
        val viewModel = buildViewModel(SavedStateHandle(), fakeRepository)

        // When
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.error)
        assertTrue(!state.isLoading)
    }

    @Test
    fun should_setEmptyFoods_when_noFoodMatchesFilter() = runTest(testDispatcher) {
        // Given: R10 — sin resultados, PantryScreen debe mostrar un estado vacío, no error
        val fakeRepository = FakeFoodRepository(allFoods = flowOf(Result.Success(emptyList())))
        val viewModel = buildViewModel(SavedStateHandle(), fakeRepository)

        // When
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.foods.isEmpty())
        assertTrue(!state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun should_populateFoods_when_queryReturnsResults() = runTest(testDispatcher) {
        // Given
        val foods = listOf(Food(name = "Leche", quantity = 1.0, category = FoodCategory.LACTEOS.label()))
        val fakeRepository = FakeFoodRepository(allFoods = flowOf(Result.Success(foods)))
        val viewModel = buildViewModel(SavedStateHandle(), fakeRepository)

        // When
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(foods, state.foods)
        assertTrue(!state.isLoading)
    }
}
