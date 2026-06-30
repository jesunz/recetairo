package com.jesunez.recetairo.feature.food.ui

import com.jesunez.recetairo.feature.food.domain.model.ProductInfo

sealed class BarcodeScanError {
    object CameraPermissionDenied : BarcodeScanError()
    object ProductNotFound : BarcodeScanError()
    object NoInternet : BarcodeScanError()
    object ServiceTimeout : BarcodeScanError()
}

data class BarcodeScanUiState(
    val isCameraPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: BarcodeScanError? = null,
    val productToPreFill: ProductInfo? = null,
    val navigateToEmptyForm: Boolean = false
)
