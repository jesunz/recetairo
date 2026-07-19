package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.PantryFilter
import com.jesunez.recetairo.feature.food.domain.usecase.GetAllFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetExpiringSoonFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetFoodsByCategoryUseCase
import com.jesunez.recetairo.feature.food.ui.PantryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PantryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllFoodsUseCase: GetAllFoodsUseCase,
    private val getFoodsByCategoryUseCase: GetFoodsByCategoryUseCase,
    private val getExpiringSoonFoodsUseCase: GetExpiringSoonFoodsUseCase
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
                _uiState.value = when (result) {
                    is Result.Success -> PantryUiState(
                        filter = filter,
                        foods = result.data,
                        isLoading = false
                    )
                    is Result.Error -> PantryUiState(
                        filter = filter,
                        isLoading = false,
                        error = result.message ?: "No se ha podido cargar la despensa."
                    )
                    is Result.Loading -> _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    private fun resolveFlow(filter: PantryFilter): Flow<Result<List<Food>>> = when (filter) {
        PantryFilter.All -> getAllFoodsUseCase()
        is PantryFilter.ByCategory -> getFoodsByCategoryUseCase(filter.category)
        PantryFilter.ExpiringSoon -> getExpiringSoonFoodsUseCase(limit = null)
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
