// Feature: insertar-alimentos-despensa, T31
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.AiFoodExtractionRepository
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import com.jesunez.recetairo.feature.food.domain.usecase.InsertFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.InsertMultipleFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.ProcessReceiptOcrUseCase
import com.jesunez.recetairo.feature.food.ui.OcrError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private var nextOcrResult: suspend (ByteArray) -> Result<List<OcrFoodItem>> =
        { Result.Success(emptyList()) }

    private var lastOcrItems: List<OcrFoodItem> = emptyList()

    private val fakeOcrRepository = object : OcrRepository {
        override suspend fun extractItemsFromImage(imageBytes: ByteArray): Result<List<OcrFoodItem>> {
            val result = nextOcrResult(imageBytes)
            if (result is Result.Success) lastOcrItems = result.data
            return result
        }
    }

    private val fakeAiFoodExtractionRepository = object : AiFoodExtractionRepository {
        override suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>> =
            Result.Success(lastOcrItems)
    }

    private var nextInsertResult: (Food) -> Result<Unit> = { Result.Success(Unit) }

    private val fakeFoodRepository = object : FoodRepository {
        override suspend fun insertFood(food: Food): Result<Unit> = nextInsertResult(food)
        override suspend fun insertFoods(foods: List<Food>): Result<Unit> = Result.Success(Unit)
        override fun searchFoodNames(query: String): Flow<List<String>> = flowOf(emptyList())
    }

    private lateinit var viewModel: ReceiptScanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ReceiptScanViewModel(
            ProcessReceiptOcrUseCase(fakeOcrRepository, fakeAiFoodExtractionRepository),
            InsertMultipleFoodsUseCase(InsertFoodUseCase(fakeFoodRepository))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region R25 — timeout de 30 segundos

    @Test
    fun should_setTimeoutError_when_ocrProcessingExceeds30Seconds() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { delay(40_000.milliseconds); Result.Success(emptyList()) }

        // When
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()

        // Then
        assertEquals(OcrError.Timeout, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    // endregion

    // region R22 — lista vacía extraída por OCR

    @Test
    fun should_setNoItemsExtractedError_when_ocrReturnsEmptyList() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(emptyList()) }

        // When
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()

        // Then
        assertEquals(OcrError.NoItemsExtracted, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    // endregion

    // region R24 — resumen de inserción múltiple

    @Test
    fun should_produceInsertionSummaryWithAllSuccesses_when_allItemsInsertSuccessfully() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manzana"), ocrItem("Pan"))) }
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()
        nextInsertResult = { Result.Success(Unit) }

        // When
        viewModel.onConfirmSelection()
        advanceUntilIdle()

        // Then
        val summary = viewModel.uiState.value.insertionSummary
        assertEquals(2, summary?.successCount)
        assertTrue(summary?.failures.isNullOrEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun should_produceInsertionSummaryWithFailures_when_someItemsFailToInsert() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manzana"), ocrItem("Pan"))) }
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()
        nextInsertResult = { food ->
            if (food.name == "Pan") Result.Error(Exception("fallo simulado"), "fallo simulado") else Result.Success(Unit)
        }

        // When
        viewModel.onConfirmSelection()
        advanceUntilIdle()

        // Then
        val summary = viewModel.uiState.value.insertionSummary
        assertEquals(1, summary?.successCount)
        assertEquals(1, summary?.failures?.size)
        assertEquals("Pan", summary?.failures?.first()?.food?.name)
    }

    @Test
    fun should_excludeUnselectedItems_when_confirmingSelection() = runTest(testDispatcher) {
        // Given
        nextOcrResult = {
            Result.Success(listOf(ocrItem("Manzana"), ocrItem("Pan").copy(isSelected = false)))
        }
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()
        nextInsertResult = { Result.Success(Unit) }

        // When
        viewModel.onConfirmSelection()
        advanceUntilIdle()

        // Then
        val summary = viewModel.uiState.value.insertionSummary
        assertEquals(1, summary?.successCount)
        assertTrue(summary?.failures.isNullOrEmpty())
    }

    // endregion

    private fun ocrItem(name: String) = OcrFoodItem(
        name = name,
        quantity = "1",
        expiryDate = "31/12/2026",
        confidence = 0.9f,
        isVerified = true
    )
}
