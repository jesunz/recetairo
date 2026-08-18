// Feature: gestion-despensa, T8
package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.usecase.DeleteFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetFoodByIdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeFoodRepository(
        private val foodById: Flow<Result<Food?>> = flowOf(Result.Success(null)),
        private val deleteFoodResult: Result<Unit> = Result.Success(Unit)
    ) : FoodRepository {
        var lastDeletedId: Long? = null

        override suspend fun insertFood(food: Food): Result<Unit> =
            throw UnsupportedOperationException()
        override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
            throw UnsupportedOperationException()
        override fun searchFoodNames(query: String): Flow<List<String>> =
            throw UnsupportedOperationException()
        override fun getAllFoods(): Flow<Result<List<Food>>> =
            throw UnsupportedOperationException()
        override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
            throw UnsupportedOperationException()
        override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
            throw UnsupportedOperationException()
        override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
            throw UnsupportedOperationException()
        override suspend fun deleteFood(foodId: Long): Result<Unit> {
            lastDeletedId = foodId
            return deleteFoodResult
        }
        override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> =
            throw UnsupportedOperationException()
        override fun getFoodById(foodId: Long): Flow<Result<Food?>> = foodById
    }

    private fun buildViewModel(
        foodId: Long = 1L,
        fakeRepository: FakeFoodRepository = FakeFoodRepository()
    ): FoodDetailViewModel = FoodDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("foodId" to foodId)),
        getFoodByIdUseCase = GetFoodByIdUseCase(fakeRepository),
        deleteFoodUseCase = DeleteFoodUseCase(fakeRepository)
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
    fun should_loadFood_when_idExists() = runTest(testDispatcher) {
        // Given
        val food = Food(id = 1L, name = "Leche", quantity = 1.0)
        val fakeRepository = FakeFoodRepository(foodById = flowOf(Result.Success(food)))

        // When
        val viewModel = buildViewModel(foodId = 1L, fakeRepository = fakeRepository)
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(food, state.food)
        assertTrue(!state.isLoading)
        assertTrue(!state.notFound)
    }

    @Test
    fun should_setNotFound_when_foodDeletedExternally() = runTest(testDispatcher) {
        // Given: el alimento existe al abrir la pantalla, pero se borra mientras está abierta
        val food = Food(id = 1L, name = "Leche", quantity = 1.0)
        val foodByIdFlow = MutableSharedFlow<Result<Food?>>(replay = 1)
        foodByIdFlow.tryEmit(Result.Success(food))
        val fakeRepository = FakeFoodRepository(foodById = foodByIdFlow)
        val viewModel = buildViewModel(foodId = 1L, fakeRepository = fakeRepository)
        runCurrent()
        assertEquals(food, viewModel.uiState.value.food)

        // When
        foodByIdFlow.tryEmit(Result.Success(null))
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.notFound)
        assertTrue(!state.isLoading)
    }

    @Test
    fun should_navigateBack_when_deleteConfirmed() = runTest(testDispatcher) {
        // Given
        val food = Food(id = 1L, name = "Leche", quantity = 1.0)
        val fakeRepository = FakeFoodRepository(
            foodById = flowOf(Result.Success(food)),
            deleteFoodResult = Result.Success(Unit)
        )
        val viewModel = buildViewModel(foodId = 1L, fakeRepository = fakeRepository)
        runCurrent()
        viewModel.onDeleteClicked()

        // When
        viewModel.onDeleteConfirmed()
        runCurrent()

        // Then
        assertEquals(1L, fakeRepository.lastDeletedId)
        val state = viewModel.uiState.value
        assertTrue(state.navigateBack)
        assertTrue(!state.showDeleteConfirmation)
    }

    @Test
    fun should_showDeleteConfirmation_when_deleteClicked() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()

        // When
        viewModel.onDeleteClicked()

        // Then
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun should_hideDeleteConfirmation_when_deleteCancelled() = runTest(testDispatcher) {
        // Given
        val viewModel = buildViewModel()
        runCurrent()
        viewModel.onDeleteClicked()

        // When
        viewModel.onDeleteCancelled()

        // Then
        val state = viewModel.uiState.value
        assertTrue(!state.showDeleteConfirmation)
        assertTrue(!state.navigateBack)
    }

    @Test
    fun should_keepDialogOpenWithoutNavigating_when_deleteFails() = runTest(testDispatcher) {
        // Given
        val exception = RuntimeException("db error")
        val fakeRepository = FakeFoodRepository(
            foodById = flowOf(Result.Success(Food(id = 1L, name = "Leche", quantity = 1.0))),
            deleteFoodResult = Result.Error(exception)
        )
        val viewModel = buildViewModel(foodId = 1L, fakeRepository = fakeRepository)
        runCurrent()
        viewModel.onDeleteClicked()

        // When
        viewModel.onDeleteConfirmed()
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertTrue(!state.navigateBack)
        assertTrue(!state.showDeleteConfirmation)
    }
}
