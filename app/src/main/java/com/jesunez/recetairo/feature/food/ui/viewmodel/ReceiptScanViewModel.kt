package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.usecase.InsertMultipleFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.ProcessReceiptOcrUseCase
import com.jesunez.recetairo.feature.food.ui.OcrError
import com.jesunez.recetairo.feature.food.ui.ReceiptScanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val processReceiptOcrUseCase: ProcessReceiptOcrUseCase,
    private val insertMultipleFoodsUseCase: InsertMultipleFoodsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScanUiState())
    val uiState: StateFlow<ReceiptScanUiState> = _uiState.asStateFlow()

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(isCameraPermissionGranted = granted) }
    }

    fun onImageCaptured(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isClassifying = false,
                    error = null,
                    items = emptyList(),
                    insertionSummary = null
                )
            }
            val result = processReceiptOcrUseCase(imageBytes) {
                _uiState.update { it.copy(isLoading = false, isClassifying = true) }
            }
            when (result) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isClassifying = false,
                        items = result.data.items,
                        error = when {
                            result.data.items.isEmpty() -> OcrError.NoItemsExtracted
                            result.data.isDegradedMode -> OcrError.AiServiceUnavailable
                            else -> null
                        }
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, isClassifying = false, error = result.exception.toOcrError())
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onItemEdited(index: Int, updatedItem: OcrFoodItem) {
        _uiState.update { state ->
            state.copy(items = state.items.toMutableList().apply { set(index, updatedItem) })
        }
    }

    fun onItemRemoved(index: Int) {
        _uiState.update { state ->
            state.copy(items = state.items.toMutableList().apply { removeAt(index) })
        }
    }

    fun onItemCategoryChanged(index: Int, category: FoodCategory) {
        _uiState.update { state ->
            state.copy(
                items = state.items.toMutableList()
                    .apply { set(index, get(index).copy(category = category)) }
            )
        }
    }

    fun onConfirmSelection() {
        val selectedFoods = _uiState.value.items.filter { it.isSelected }.map { it.toFood() }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val summary = insertMultipleFoodsUseCase(selectedFoods)
            _uiState.update { it.copy(isLoading = false, insertionSummary = summary) }
        }
    }

    private fun Throwable.toOcrError(): OcrError = when (this) {
        is TimeoutCancellationException -> OcrError.Timeout
        else -> OcrError.NoItemsExtracted
    }

    private fun OcrFoodItem.toFood(): Food = Food(
        name = name,
        quantity = quantity.replace(',', '.').toDoubleOrNull() ?: 0.0,
        category = category.label(),
        expiryDate = runCatching { LocalDate.parse(expiryDate, DATE_FORMATTER) }.getOrNull()
    )

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
