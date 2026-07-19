package com.jesunez.recetairo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.usecase.GetCategorySummariesUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.GetExpiringSoonFoodsUseCase
import com.jesunez.recetairo.ui.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCategorySummariesUseCase: GetCategorySummariesUseCase,
    private val getExpiringSoonFoodsUseCase: GetExpiringSoonFoodsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        collectPantryData()
    }

    private fun collectPantryData() {
        viewModelScope.launch {
            combine(
                getCategorySummariesUseCase(),
                getExpiringSoonFoodsUseCase(limit = EXPIRING_SOON_HOME_LIMIT)
            ) { summariesResult, expiringResult ->
                when {
                    summariesResult is Result.Error -> HomeUiState(
                        isLoading = false,
                        error = summariesResult.message ?: "No se ha podido cargar la despensa."
                    )
                    expiringResult is Result.Error -> HomeUiState(
                        isLoading = false,
                        error = expiringResult.message ?: "No se ha podido cargar la despensa."
                    )
                    summariesResult is Result.Success && expiringResult is Result.Success -> HomeUiState(
                        categorySummaries = summariesResult.data,
                        expiringSoonFoods = expiringResult.data,
                        isLoading = false
                    )
                    else -> _uiState.value.copy(isLoading = true)
                }
            }.collect { newState -> _uiState.value = newState }
        }
    }

    private companion object {
        const val EXPIRING_SOON_HOME_LIMIT = 5
    }
}
