// Feature: insertar-alimentos-despensa, T31
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
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

    private var nextAiResult: suspend () -> Result<List<OcrFoodItem>> = { Result.Success(lastOcrItems) }

    private val fakeAiFoodExtractionRepository = object : AiFoodExtractionRepository {
        override suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>> = nextAiResult()
    }

    private var nextInsertResult: (Food) -> Result<Unit> = { Result.Success(Unit) }

    private val fakeFoodRepository = object : FoodRepository {
        override suspend fun insertFood(food: Food): Result<Unit> = nextInsertResult(food)
        override suspend fun insertFoods(foods: List<Food>): Result<Unit> = Result.Success(Unit)
        override fun searchFoodNames(query: String): Flow<List<String>> = flowOf(emptyList())
        override fun getAllFoods(): Flow<Result<List<Food>>> = flowOf(Result.Success(emptyList()))
        override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
            flowOf(Result.Success(emptyList()))
        override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
            flowOf(Result.Success(emptyList()))
        override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
            flowOf(Result.Success(emptyList()))
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

    // region R6 — edición de categoría por ítem

    @Test
    fun should_updateItemCategory_when_categoryEditedByIndex() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manzana"), ocrItem("Pan"))) }
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()

        // When
        viewModel.onItemCategoryChanged(0, FoodCategory.FRUTAS)

        // Then
        val items = viewModel.uiState.value.items
        assertEquals(FoodCategory.FRUTAS, items[0].category)
        assertEquals("Manzana", items[0].name)
        assertEquals(FoodCategory.OTROS, items[1].category)
    }

    // endregion

    // region R7 — aviso de Modo_Degradado

    @Test
    fun should_setAiServiceUnavailableError_when_aiExtractionFails() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manzana"))) }
        nextAiResult = { Result.Error(Exception("fallo IA"), "fallo IA") }

        // When
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()

        // Then
        assertEquals(OcrError.AiServiceUnavailable, viewModel.uiState.value.error)
        assertEquals(FoodCategory.OTROS, viewModel.uiState.value.items.first().category)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isClassifying)
    }

    @Test
    fun should_setNoError_when_aiExtractionSucceeds() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manzana"))) }
        nextAiResult = { Result.Success(listOf(ocrItem("Manzana").copy(category = FoodCategory.FRUTAS))) }

        // When
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(FoodCategory.FRUTAS, viewModel.uiState.value.items.first().category)
    }

    // endregion

    // region R13 — indicador isClassifying durante la llamada al Extractor_IA

    @Test
    fun should_setIsClassifyingTrueAndIsLoadingFalse_when_ocrCompletesAndAiIsStillRunning() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manzana"))) }
        nextAiResult = { delay(5_000.milliseconds); Result.Success(lastOcrItems) }

        // When
        viewModel.onImageCaptured(ByteArray(0))
        testDispatcher.scheduler.runCurrent()

        // Then
        assertTrue(viewModel.uiState.value.isClassifying)
        assertFalse(viewModel.uiState.value.isLoading)

        // When (AI extractor finishes)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isClassifying)
        assertFalse(viewModel.uiState.value.items.isEmpty())
    }

    // endregion

    // region T12 — items con needsReview=true siguen editables/seleccionables/insertables (R4, R5)

    @Test
    fun should_updateNeedsReviewItemLikeAnyOther_when_edited() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manz").copy(needsReview = true))) }
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()

        // When
        val edited = viewModel.uiState.value.items[0].copy(name = "Manzana")
        viewModel.onItemEdited(0, edited)

        // Then
        val item = viewModel.uiState.value.items[0]
        assertEquals("Manzana", item.name)
        assertTrue("T12: editing a needsReview item must not clear the flag", item.needsReview)
    }

    @Test
    fun should_toggleSelectionOfNeedsReviewItem_when_edited() = runTest(testDispatcher) {
        // Given
        nextOcrResult = { Result.Success(listOf(ocrItem("Manzana").copy(needsReview = true))) }
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()
        val original = viewModel.uiState.value.items[0]
        assertTrue(original.isSelected)

        // When
        viewModel.onItemEdited(0, original.copy(isSelected = false))

        // Then
        assertFalse(
            "T12: needsReview items must remain selectable/deselectable like any other item",
            viewModel.uiState.value.items[0].isSelected
        )
    }

    @Test
    fun should_insertNeedsReviewItem_when_selectedAndConfirmed() = runTest(testDispatcher) {
        // Given
        nextOcrResult = {
            Result.Success(listOf(ocrItem("Manzana").copy(needsReview = true), ocrItem("Pan")))
        }
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()
        nextInsertResult = { Result.Success(Unit) }

        // When
        viewModel.onConfirmSelection()
        advanceUntilIdle()

        // Then
        val summary = viewModel.uiState.value.insertionSummary
        assertEquals(
            "T12: a needsReview item must be insertable exactly like any other selected item",
            2,
            summary?.successCount
        )
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
