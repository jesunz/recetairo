// Feature: consultar-despensa, T14
package com.jesunez.recetairo.ui.viewmodel

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.usecase.GetCategorySummariesUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetExpiringSoonFoodsUseCase
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeFoodRepository(
        private val categorySummaries: Flow<Result<List<CategorySummary>>>,
        private val expiringSoonFoods: Flow<Result<List<Food>>>
    ) : FoodRepository {
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
        override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> = expiringSoonFoods
        override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> = categorySummaries
    }

    private fun buildViewModel(
        categorySummaries: Flow<Result<List<CategorySummary>>>,
        expiringSoonFoods: Flow<Result<List<Food>>>
    ): HomeViewModel {
        val fakeRepository = FakeFoodRepository(categorySummaries, expiringSoonFoods)
        return HomeViewModel(
            getCategorySummariesUseCase = GetCategorySummariesUseCase(fakeRepository),
            getExpiringSoonFoodsUseCase = GetExpiringSoonFoodsUseCase(fakeRepository)
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun should_showLoading_when_viewModelIsInitialized() {
        // Given / When — el ViewModel se construye pero el dispatcher aún no ha avanzado
        val viewModel = buildViewModel(
            categorySummaries = flowOf(Result.Success(emptyList())),
            expiringSoonFoods = flowOf(Result.Success(emptyList()))
        )

        // Then
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun should_setError_when_categorySummariesFails() = runTest(testDispatcher) {
        // Given
        val errorMessage = "No se ha podido cargar las categorías."
        val viewModel = buildViewModel(
            categorySummaries = flowOf(Result.Error(RuntimeException("db error"), errorMessage)),
            expiringSoonFoods = flowOf(Result.Success(emptyList()))
        )

        // When
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.error)
        assertTrue(!state.isLoading)
    }

    @Test
    fun should_setError_when_expiringSoonFoodsFails() = runTest(testDispatcher) {
        // Given
        val errorMessage = "No se han podido cargar los próximos a vencer."
        val viewModel = buildViewModel(
            categorySummaries = flowOf(Result.Success(emptyList())),
            expiringSoonFoods = flowOf(Result.Error(RuntimeException("db error"), errorMessage))
        )

        // When
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.error)
        assertTrue(!state.isLoading)
    }

    @Test
    fun should_populateState_when_bothQueriesSucceed() = runTest(testDispatcher) {
        // Given
        val summaries = listOf(CategorySummary(FoodCategory.LACTEOS, 3))
        val expiring = listOf(Food(name = "Leche", quantity = 1.0, category = FoodCategory.LACTEOS.label()))
        val viewModel = buildViewModel(
            categorySummaries = flowOf(Result.Success(summaries)),
            expiringSoonFoods = flowOf(Result.Success(expiring))
        )

        // When
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertEquals(summaries, state.categorySummaries)
        assertEquals(expiring, state.expiringSoonFoods)
        assertTrue(!state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun should_haveEmptyExpiringSoonFoods_when_noFoodsAreExpiringSoon() = runTest(testDispatcher) {
        // Given: R14 — sin alimentos próximos a vencer, HomeScreen debe poder ocultar la sección
        val summaries = listOf(CategorySummary(FoodCategory.LACTEOS, 1))
        val viewModel = buildViewModel(
            categorySummaries = flowOf(Result.Success(summaries)),
            expiringSoonFoods = flowOf(Result.Success(emptyList()))
        )

        // When
        runCurrent()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.expiringSoonFoods.isEmpty())
        assertTrue(!state.isLoading)
    }
}
