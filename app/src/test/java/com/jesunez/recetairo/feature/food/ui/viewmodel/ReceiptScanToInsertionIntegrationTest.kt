// Feature: insertar-alimentos-despensa, T33 — Integration: OCR ticket -> editable list -> summary
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Integration test covering Flow 3 del diseño (design.md): ReceiptScanViewModel procesa una
 * imagen de ticket vía ProcessReceiptOcrUseCase (real, con OcrRepository fake) -> el Usuario
 * edita y elimina ítems de la lista extraída -> InsertMultipleFoodsUseCase (real, con
 * InsertFoodUseCase real y FoodRepository fake) persiste la selección y produce el resumen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptScanToInsertionIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val insertedFoods = mutableListOf<Food>()
    private var failingFoodName: String? = null

    private val ocrItems = listOf(
        OcrFoodItem(name = "Manzanas", quantity = "6", expiryDate = "31/12/2026", confidence = 0.95f, isVerified = true),
        OcrFoodItem(name = "Pan de molde", quantity = "1", expiryDate = "15/08/2026", confidence = 0.88f, isVerified = true),
        OcrFoodItem(name = "Yogur", quantity = "4", expiryDate = "01/09/2026", confidence = 0.75f, isVerified = true)
    )

    private val fakeOcrRepository = object : OcrRepository {
        override suspend fun extractItemsFromImage(imageBytes: ByteArray): Result<List<OcrFoodItem>> =
            Result.Success(ocrItems)
    }

    private val fakeAiFoodExtractionRepository = object : AiFoodExtractionRepository {
        override suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>> =
            Result.Success(ocrItems)
    }

    private val fakeFoodRepository = object : FoodRepository {
        override suspend fun insertFood(food: Food): Result<Unit> {
            if (food.name == failingFoodName) {
                return Result.Error(Exception("fallo simulado"), "No se ha podido guardar el alimento. Inténtalo de nuevo.")
            }
            insertedFoods.add(food)
            return Result.Success(Unit)
        }
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

    // region R21, R24 — lista editable y resumen de inserción

    @Test
    fun should_persistEditedAndKeepUnmodifiedSelectedItems_when_confirmingAfterEditingList() = runTest(testDispatcher) {
        // Given: el OCR extrae 3 ítems
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.items.size)

        // When: el usuario edita el nombre y la cantidad del primer ítem (R21)
        val editedApples = viewModel.uiState.value.items[0].copy(name = "Manzanas Golden", quantity = "8")
        viewModel.onItemEdited(0, editedApples)

        // And: el usuario elimina el segundo ítem (R21)
        viewModel.onItemRemoved(1)

        // Then: la lista refleja la edición y la eliminación
        val editedList = viewModel.uiState.value.items
        assertEquals(2, editedList.size)
        assertEquals("Manzanas Golden", editedList[0].name)
        assertEquals("Yogur", editedList[1].name)

        // When: el usuario confirma la selección
        viewModel.onConfirmSelection()
        advanceUntilIdle()

        // Then: se insertan los ítems restantes con los datos editados (R23) y el resumen es correcto (R24)
        val summary = viewModel.uiState.value.insertionSummary
        requireNotNull(summary)
        assertEquals(2, summary.successCount)
        assertTrue(summary.failures.isEmpty())
        assertEquals(2, insertedFoods.size)
        assertEquals("Manzanas Golden", insertedFoods[0].name)
        assertEquals(8.0, insertedFoods[0].quantity, 0.0)
        assertEquals("Yogur", insertedFoods[1].name)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun should_reportFailureInSummaryWithoutBlockingOthers_when_oneItemFailsToPersist() = runTest(testDispatcher) {
        // Given: el OCR extrae 3 ítems y uno de ellos fallará al persistir
        failingFoodName = "Pan de molde"
        viewModel.onImageCaptured(ByteArray(0))
        advanceUntilIdle()

        // When: el usuario confirma la lista completa tal cual, sin editar
        viewModel.onConfirmSelection()
        advanceUntilIdle()

        // Then: el resumen distingue éxitos de fallos, con el nombre y motivo del fallo (R24)
        val summary = viewModel.uiState.value.insertionSummary
        requireNotNull(summary)
        assertEquals(2, summary.successCount)
        assertEquals(1, summary.failures.size)
        assertEquals("Pan de molde", summary.failures.first().food.name)
        assertEquals(2, insertedFoods.size)
    }

    // endregion
}
