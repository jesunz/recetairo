// Feature: insertar-alimentos-despensa, T33 — Integration: barcode scan -> pre-filled form -> save
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.google.mlkit.vision.barcode.common.Barcode
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.model.SaveResult
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.repository.ProductRepository
import com.jesunez.recetairo.feature.food.domain.usecase.InsertFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.LookupProductByBarcodeUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.SearchFoodHistoryUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.ValidateFoodUseCase
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
import org.junit.Before
import org.junit.Test

/**
 * Integration test covering Flow 2 del diseño (design.md): BarcodeScanViewModel detecta un
 * código -> LookupProductByBarcodeUseCase (real) consulta ProductRepository (fake) -> el
 * resultado se traslada a AddFoodViewModel.onProductInfoReceived (simulando la navegación
 * BarcodeScanScreen -> AddFoodScreen) -> el Usuario completa la cantidad -> ValidateFoodUseCase
 * e InsertFoodUseCase (reales) persisten en FoodRepository (fake).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeScanToAddFoodIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val insertedFoods = mutableListOf<Food>()

    private val fakeProductRepository = object : ProductRepository {
        override suspend fun getProductByBarcode(barcode: String): Result<ProductInfo> = Result.Success(
            ProductInfo(barcode = barcode, name = "Leche entera", brand = "Marca X", category = "Lácteos", imageUrl = "http://img/leche.png")
        )
    }

    private val fakeFoodRepository = object : FoodRepository {
        override suspend fun insertFood(food: Food): Result<Unit> {
            insertedFoods.add(food)
            return Result.Success(Unit)
        }
        override suspend fun insertFoods(foods: List<Food>): Result<Unit> = Result.Success(Unit)
        override fun searchFoodNames(query: String): Flow<List<String>> = flowOf(emptyList())
    }

    private lateinit var barcodeScanViewModel: BarcodeScanViewModel
    private lateinit var addFoodViewModel: AddFoodViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        barcodeScanViewModel = BarcodeScanViewModel(LookupProductByBarcodeUseCase(fakeProductRepository))
        addFoodViewModel = AddFoodViewModel(
            ValidateFoodUseCase(),
            InsertFoodUseCase(fakeFoodRepository),
            SearchFoodHistoryUseCase(fakeFoodRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region R15 — pre-relleno del formulario y guardado posterior

    @Test
    fun should_prefillFormAndSaveFood_when_barcodeScanFindsProductAndUserCompletesQuantity() = runTest(testDispatcher) {
        // When: se detecta un código de barras válido
        barcodeScanViewModel.onBarcodeDetected("7501234567890", Barcode.FORMAT_EAN_13)
        advanceUntilIdle()

        // Then: el producto encontrado queda listo para pre-rellenar el formulario
        val productToPreFill = barcodeScanViewModel.uiState.value.productToPreFill
        requireNotNull(productToPreFill)

        // When: la navegación traslada el producto al formulario (BarcodeScanScreen -> AddFoodScreen)
        addFoodViewModel.onProductInfoReceived(productToPreFill)

        // Then: los campos recibidos quedan pre-rellenados y editables (R15)
        val prefilledState = addFoodViewModel.uiState.value
        assertEquals("Leche entera", prefilledState.name)
        assertEquals("Marca X", prefilledState.brand)
        assertEquals("Lácteos", prefilledState.category)
        assertEquals("http://img/leche.png", prefilledState.imageUrl)

        // When: el usuario revisa/completa el resto de campos obligatorios y guarda
        addFoodViewModel.onQuantityChanged("2")
        addFoodViewModel.onSaveClicked()
        advanceUntilIdle()

        // Then: el alimento se persiste con los datos pre-rellenados y el guardado tiene éxito
        assertEquals(SaveResult.Success, addFoodViewModel.uiState.value.saveResult)
        assertEquals(1, insertedFoods.size)
        assertEquals("Leche entera", insertedFoods.first().name)
        assertEquals(2.0, insertedFoods.first().quantity, 0.0)
    }

    // endregion
}
