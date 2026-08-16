// Feature: insertar-alimentos-despensa, T27
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.google.mlkit.vision.barcode.common.Barcode
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.exception.ProductNotFoundException
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.repository.ProductRepository
import com.jesunez.recetairo.feature.food.domain.usecase.LookupProductByBarcodeUseCase
import com.jesunez.recetairo.feature.food.ui.BarcodeScanError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private var nextLookupResult: suspend (String) -> Result<ProductInfo> =
        { Result.Success(ProductInfo(barcode = it, name = "Test", brand = null, category = null, imageUrl = null)) }

    private val fakeRepository = object : ProductRepository {
        override suspend fun getProductByBarcode(barcode: String): Result<ProductInfo> =
            nextLookupResult(barcode)
    }

    private lateinit var viewModel: BarcodeScanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BarcodeScanViewModel(LookupProductByBarcodeUseCase(fakeRepository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region R13 — permiso de cámara denegado

    @Test
    fun should_setCameraPermissionDeniedError_when_permissionIsDenied() {
        // When
        viewModel.onCameraPermissionResult(granted = false)

        // Then
        assertFalse(viewModel.uiState.value.isCameraPermissionGranted)
        assertEquals(BarcodeScanError.CameraPermissionDenied, viewModel.uiState.value.error)
    }

    @Test
    fun should_grantPermissionWithoutError_when_permissionIsGranted() {
        // When
        viewModel.onCameraPermissionResult(granted = true)

        // Then
        assertTrue(viewModel.uiState.value.isCameraPermissionGranted)
        assertEquals(null, viewModel.uiState.value.error)
    }

    // endregion

    // region Detecciones duplicadas de la cámara tras resolver un código

    @Test
    fun should_invokeUseCaseOnce_when_sameBarcodeDetectedTwiceAfterResolution() = runTest(testDispatcher) {
        // Given: la cámara sigue analizando fotogramas y vuelve a detectar el mismo código
        // después de que la búsqueda ya se haya resuelto (caso real: navegación en curso).
        var callCount = 0
        val countingRepository = object : ProductRepository {
            override suspend fun getProductByBarcode(barcode: String): Result<ProductInfo> {
                callCount++
                return Result.Success(ProductInfo(barcode = barcode, name = "Test", brand = null, category = null, imageUrl = null))
            }
        }
        val viewModel = BarcodeScanViewModel(LookupProductByBarcodeUseCase(countingRepository))

        // When
        viewModel.onBarcodeDetected("1234567890123", Barcode.FORMAT_EAN_13)
        runCurrent()
        viewModel.onBarcodeDetected("1234567890123", Barcode.FORMAT_EAN_13)
        runCurrent()

        // Then: solo se dispara una búsqueda (y por tanto una navegación) por instancia de pantalla
        assertEquals(1, callCount)
    }

    // endregion

    // region R16 — producto no encontrado

    @Test
    fun should_setProductNotFoundError_when_repositoryThrowsProductNotFound() = runTest(testDispatcher) {
        // Given
        nextLookupResult = { throw ProductNotFoundException(it) }

        // When
        viewModel.onBarcodeDetected("1234567890123", Barcode.FORMAT_EAN_13)
        runCurrent()

        // Then
        assertEquals(BarcodeScanError.ProductNotFound, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.navigateToEmptyForm)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // endregion

    // region R17 — sin conexión a internet

    @Test
    fun should_setNoInternetError_when_repositoryThrowsIOException() = runTest(testDispatcher) {
        // Given
        nextLookupResult = { throw IOException("sin conexión") }

        // When
        viewModel.onBarcodeDetected("1234567890123", Barcode.FORMAT_EAN_13)
        runCurrent()

        // Then
        assertEquals(BarcodeScanError.NoInternet, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.navigateToEmptyForm)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // endregion

    // region R18 — timeout de 10 segundos

    @Test
    fun should_setServiceTimeoutError_when_repositoryThrowsSocketTimeout() = runTest(testDispatcher) {
        // Given
        nextLookupResult = { throw SocketTimeoutException("timeout tras 10s") }

        // When
        viewModel.onBarcodeDetected("1234567890123", Barcode.FORMAT_EAN_13)
        runCurrent()

        // Then
        assertEquals(BarcodeScanError.ServiceTimeout, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.navigateToEmptyForm)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // endregion
}
