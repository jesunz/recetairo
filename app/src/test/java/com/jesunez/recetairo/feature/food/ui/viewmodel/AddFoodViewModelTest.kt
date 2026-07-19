// Feature: insertar-alimentos-despensa, T23
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.FoodField
import com.jesunez.recetairo.feature.food.domain.model.SaveResult
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.usecase.InsertFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.SearchFoodHistoryUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.ValidateFoodUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private var nextSearchResults: List<String> = emptyList()
    private var nextInsertResult: Result<Unit> = Result.Success(Unit)

    private val fakeRepository = object : FoodRepository {
        override suspend fun insertFood(food: Food): Result<Unit> = nextInsertResult
        override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
            throw UnsupportedOperationException()
        override fun searchFoodNames(query: String): Flow<List<String>> =
            flowOf(nextSearchResults)
        override fun getAllFoods(): Flow<Result<List<Food>>> = flowOf(Result.Success(emptyList()))
        override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
            flowOf(Result.Success(emptyList()))
        override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
            flowOf(Result.Success(emptyList()))
        override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
            flowOf(Result.Success(emptyList()))
    }

    private lateinit var viewModel: AddFoodViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddFoodViewModel(
            validateFoodUseCase = ValidateFoodUseCase(),
            insertFoodUseCase = InsertFoodUseCase(fakeRepository),
            searchFoodHistoryUseCase = SearchFoodHistoryUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region autocompletado con debounce

    @Test
    fun should_emitSuggestions_when_nameChangedAfterDebounce() = runTest(testDispatcher) {
        // Given
        nextSearchResults = listOf("Leche", "Lentejas")
        runCurrent()

        // When
        viewModel.onNameChanged("Le")
        advanceTimeBy(301.milliseconds)
        runCurrent()

        // Then
        assertEquals(listOf("Leche", "Lentejas"), viewModel.uiState.value.nameSuggestions)
    }

    @Test
    fun should_notEmitSuggestions_when_debounceNotElapsed() = runTest(testDispatcher) {
        // Given
        nextSearchResults = listOf("Leche")
        runCurrent()

        // When — solo 200 ms, menos del umbral de 300 ms
        viewModel.onNameChanged("Le")
        advanceTimeBy(200.milliseconds)
        runCurrent()

        // Then
        assertEquals(emptyList<String>(), viewModel.uiState.value.nameSuggestions)
    }

    @Test
    fun should_clearSuggestions_when_nameIsEmpty() = runTest(testDispatcher) {
        // Given — primero aparecen sugerencias para "Le"
        nextSearchResults = listOf("Leche")
        runCurrent()
        viewModel.onNameChanged("Le")
        advanceTimeBy(301.milliseconds)
        runCurrent()

        // When — el usuario borra el texto
        viewModel.onNameChanged("")
        advanceTimeBy(301.milliseconds)
        runCurrent()

        // Then
        assertEquals(emptyList<String>(), viewModel.uiState.value.nameSuggestions)
    }

    // endregion

    // region errores de validación por campo

    @Test
    fun should_setNameError_when_nameIsBlank() {
        // Given
        viewModel.onNameChanged("   ")
        viewModel.onQuantityChanged("1.0")

        // When
        viewModel.onSaveClicked()

        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey(FoodField.NAME))
    }

    @Test
    fun should_setQuantityError_when_quantityTextIsNotANumber() {
        // Given
        viewModel.onNameChanged("Leche")
        viewModel.onQuantityChanged("abc")

        // When
        viewModel.onSaveClicked()

        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey(FoodField.QUANTITY))
    }

    @Test
    fun should_setExpiryDateError_when_dateFormatIsInvalid() {
        // Given
        viewModel.onNameChanged("Leche")
        viewModel.onQuantityChanged("1.0")
        viewModel.onExpiryDateChanged("2030-01-01") // formato ISO en lugar de DD/MM/AAAA

        // When
        viewModel.onSaveClicked()

        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey(FoodField.EXPIRY_DATE))
    }

    @Test
    fun should_setExpiryDateError_when_expiryDateIsInThePast() {
        // Given
        viewModel.onNameChanged("Leche")
        viewModel.onQuantityChanged("1.0")
        viewModel.onExpiryDateChanged("01/01/2020")

        // When
        viewModel.onSaveClicked()

        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey(FoodField.EXPIRY_DATE))
    }

    // endregion

    // region saveResult

    @Test
    fun should_setSaveResultSuccess_when_insertSucceeds() = runTest(testDispatcher) {
        // Given
        nextInsertResult = Result.Success(Unit)
        viewModel.onNameChanged("Leche")
        viewModel.onQuantityChanged("1.0")
        viewModel.onUnitChanged("unidades")

        // When
        viewModel.onSaveClicked()
        runCurrent()

        // Then
        assertEquals(SaveResult.Success, viewModel.uiState.value.saveResult)
    }

    @Test
    fun should_setSaveResultFailure_when_insertFails() = runTest(testDispatcher) {
        // Given
        val errorMessage = "Error al guardar"
        nextInsertResult = Result.Error(RuntimeException("db error"), errorMessage)
        viewModel.onNameChanged("Leche")
        viewModel.onQuantityChanged("1.0")
        viewModel.onUnitChanged("unidades")

        // When
        viewModel.onSaveClicked()
        runCurrent()

        // Then
        val saveResult = viewModel.uiState.value.saveResult
        assertTrue(saveResult is SaveResult.Failure)
        assertEquals(errorMessage, (saveResult as SaveResult.Failure).message)
    }

    // endregion
}
