package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.PantryFilter
import com.jesunez.recetairo.feature.food.domain.usecase.DeleteFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetAllFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetExpiringSoonFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetFoodsByCategoryUseCase
import com.jesunez.recetairo.feature.food.ui.PantryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PantryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllFoodsUseCase: GetAllFoodsUseCase,
    private val getFoodsByCategoryUseCase: GetFoodsByCategoryUseCase,
    private val getExpiringSoonFoodsUseCase: GetExpiringSoonFoodsUseCase,
    private val deleteFoodsUseCase: DeleteFoodsUseCase
) : ViewModel() {

    private val filter: PantryFilter = savedStateHandle.toPantryFilter()

    private val _uiState = MutableStateFlow(PantryUiState(filter = filter))
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init {
        collectFoods()
    }

    private fun collectFoods() {
        viewModelScope.launch {
            resolveFlow(filter).collect { result ->
                _uiState.update { state ->
                    when (result) {
                        is Result.Success -> state.copy(
                            foods = result.data,
                            isLoading = false,
                            error = null
                        )
                        is Result.Error -> state.copy(
                            isLoading = false,
                            error = result.message ?: "No se ha podido cargar la despensa."
                        )
                        is Result.Loading -> state.copy(isLoading = true)
                    }
                }
            }
        }
    }

    private fun resolveFlow(filter: PantryFilter): Flow<Result<List<Food>>> = when (filter) {
        PantryFilter.All -> getAllFoodsUseCase()
        is PantryFilter.ByCategory -> getFoodsByCategoryUseCase(filter.category)
        PantryFilter.ExpiringSoon -> getExpiringSoonFoodsUseCase(limit = null)
    }

    fun onRowLongPressed(id: Long) {
        _uiState.update { it.copy(selectedIds = it.selectedIds + id) }
    }

    fun onRowTapped(id: Long) {
        _uiState.update { state ->
            if (!state.isSelectionMode) {
                state
            } else {
                val selectedIds = if (id in state.selectedIds) {
                    state.selectedIds - id
                } else {
                    state.selectedIds + id
                }
                state.copy(selectedIds = selectedIds)
            }
        }
    }

    fun onSelectionCleared() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun onDeleteSelectedClicked() {
        _uiState.update { it.copy(pendingDeleteCount = it.selectedIds.size) }
    }

    fun onDeleteCancelled() {
        _uiState.update { it.copy(pendingDeleteCount = null) }
    }

    fun onDeleteConfirmed() {
        val idsToDelete = _uiState.value.selectedIds
        viewModelScope.launch {
            when (val result = deleteFoodsUseCase(idsToDelete.toList())) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        selectedIds = emptySet(),
                        pendingDeleteCount = null,
                        deleteResultMessage = "${idsToDelete.size} alimento(s) eliminado(s)."
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        pendingDeleteCount = null,
                        deleteResultMessage = result.message
                            ?: "No se han podido eliminar los alimentos seleccionados."
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    private companion object {
        fun SavedStateHandle.toPantryFilter(): PantryFilter {
            val categoryRaw = get<String>("category")
            val expiringSoon = get<String>("expiringSoon").toBoolean()
            return when {
                expiringSoon -> PantryFilter.ExpiringSoon
                !categoryRaw.isNullOrBlank() -> PantryFilter.ByCategory(FoodCategory.valueOf(categoryRaw))
                else -> PantryFilter.All
            }
        }
    }
}
