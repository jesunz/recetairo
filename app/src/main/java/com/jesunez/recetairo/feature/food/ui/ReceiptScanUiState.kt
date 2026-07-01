package com.jesunez.recetairo.feature.food.ui

import com.jesunez.recetairo.feature.food.domain.model.InsertionSummary
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem

sealed class OcrError {
    object Timeout : OcrError()
    object NoItemsExtracted : OcrError()
}

data class ReceiptScanUiState(
    val isCameraPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: OcrError? = null,
    val items: List<OcrFoodItem> = emptyList(),
    val insertionSummary: InsertionSummary? = null
)
