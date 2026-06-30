package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.exception.ProductNotFoundException
import com.jesunez.recetairo.feature.food.domain.usecase.LookupProductByBarcodeUseCase
import com.jesunez.recetairo.feature.food.ui.BarcodeScanError
import com.jesunez.recetairo.feature.food.ui.BarcodeScanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

@HiltViewModel
class BarcodeScanViewModel @Inject constructor(
    private val lookupProductByBarcodeUseCase: LookupProductByBarcodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeScanUiState())
    val uiState: StateFlow<BarcodeScanUiState> = _uiState.asStateFlow()

    fun onCameraPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(isCameraPermissionGranted = true, error = null) }
        } else {
            _uiState.update {
                it.copy(isCameraPermissionGranted = false, error = BarcodeScanError.CameraPermissionDenied)
            }
        }
    }

    fun onBarcodeDetected(rawValue: String, format: Int) {
        if (format != Barcode.FORMAT_EAN_8 && format != Barcode.FORMAT_EAN_13 && format != Barcode.FORMAT_UPC_A) return
        if (_uiState.value.isLoading) return
        lookupProduct(rawValue)
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(productToPreFill = null, navigateToEmptyForm = false) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    private fun lookupProduct(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = lookupProductByBarcodeUseCase(barcode)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, productToPreFill = result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exception.toBarcodeScanError(),
                        navigateToEmptyForm = true
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun Throwable.toBarcodeScanError(): BarcodeScanError = when {
        this is ProductNotFoundException -> BarcodeScanError.ProductNotFound
        this is SocketTimeoutException -> BarcodeScanError.ServiceTimeout
        this is IOException -> BarcodeScanError.NoInternet
        else -> BarcodeScanError.ProductNotFound
    }
}
