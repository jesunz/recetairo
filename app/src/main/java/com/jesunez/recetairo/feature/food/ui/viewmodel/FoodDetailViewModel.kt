package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.usecase.DeleteFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetFoodByIdUseCase
import com.jesunez.recetairo.feature.food.ui.FoodDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFoodByIdUseCase: GetFoodByIdUseCase,
    private val deleteFoodUseCase: DeleteFoodUseCase
) : ViewModel() {

    private val foodId: Long = checkNotNull(savedStateHandle["foodId"])

    private val _uiState = MutableStateFlow(FoodDetailUiState())
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()

    init {
        collectFood()
    }

    private fun collectFood() {
        viewModelScope.launch {
            getFoodByIdUseCase(foodId).collect { result ->
                _uiState.update { state ->
                    when (result) {
                        is Result.Success -> if (result.data != null) {
                            state.copy(food = result.data, isLoading = false, notFound = false)
                        } else {
                            state.copy(isLoading = false, notFound = true)
                        }
                        is Result.Error -> state.copy(isLoading = false, notFound = true)
                        is Result.Loading -> state.copy(isLoading = true)
                    }
                }
            }
        }
    }

    fun onDeleteClicked() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDeleteCancelled() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            when (deleteFoodUseCase(foodId)) {
                is Result.Success -> _uiState.update {
                    it.copy(showDeleteConfirmation = false, navigateBack = true)
                }
                is Result.Error -> _uiState.update {
                    it.copy(showDeleteConfirmation = false)
                }
                is Result.Loading -> Unit
            }
        }
    }
}
