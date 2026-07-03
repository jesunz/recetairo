// Feature: insertar-alimentos-despensa, Property 7
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.google.mlkit.vision.barcode.common.Barcode
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.repository.ProductRepository
import com.jesunez.recetairo.feature.food.domain.usecase.LookupProductByBarcodeUseCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeScanViewModelPropertyTest {

    private val acceptedFormats = listOf(
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_UPC_A
    )

    private val nonAcceptedFormats = listOf(
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_CODE_39,
        Barcode.FORMAT_CODE_93,
        Barcode.FORMAT_CODABAR,
        Barcode.FORMAT_DATA_MATRIX,
        Barcode.FORMAT_ITF,
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_UPC_E,
        Barcode.FORMAT_PDF417,
        Barcode.FORMAT_AZTEC
    )

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(onLookup: (String) -> Result<ProductInfo>): BarcodeScanViewModel {
        val fakeRepository = object : ProductRepository {
            override suspend fun getProductByBarcode(barcode: String): Result<ProductInfo> =
                onLookup(barcode)
        }
        return BarcodeScanViewModel(LookupProductByBarcodeUseCase(fakeRepository))
    }

    @Test
    fun should_invokeUseCase_when_formatIsAccepted() = runTest(testDispatcher) {
        // R14: EAN-8, EAN-13 y UPC-A deben activar la búsqueda de producto
        checkAll(100, Arb.element(acceptedFormats)) { format ->
            var callCount = 0
            val viewModel = buildViewModel { barcode ->
                callCount++
                Result.Success(
                    ProductInfo(barcode = barcode, name = "Test", brand = null, category = null, imageUrl = null)
                )
            }
            viewModel.onCameraPermissionResult(true)

            viewModel.onBarcodeDetected("1234567890123", format)
            runCurrent()

            assertEquals(
                "R14: el formato $format debería invocar el use case exactamente una vez",
                1,
                callCount
            )
        }
    }

    @Test
    fun should_notInvokeUseCase_when_formatIsNotAccepted() = runTest(testDispatcher) {
        // R14: los formatos no aceptados no deben activar la búsqueda
        checkAll(100, Arb.element(nonAcceptedFormats)) { format ->
            var callCount = 0
            val viewModel = buildViewModel { barcode ->
                callCount++
                Result.Success(
                    ProductInfo(barcode = barcode, name = "Test", brand = null, category = null, imageUrl = null)
                )
            }
            viewModel.onCameraPermissionResult(true)

            viewModel.onBarcodeDetected("1234567890123", format)
            runCurrent()

            assertEquals(
                "R14: el formato $format NO debe invocar el use case",
                0,
                callCount
            )
        }
    }
}
