package com.jesunez.recetairo.feature.recipe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.usecase.GetSavedRecipesUseCase
import com.jesunez.recetairo.feature.recipe.ui.RecipeListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val getSavedRecipesUseCase: GetSavedRecipesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")

    init {
        collectRecipes()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectRecipes() {
        viewModelScope.launch {
            query
                .flatMapLatest { getSavedRecipesUseCase(it) }
                .collect { result ->
                    _uiState.update { state ->
                        when (result) {
                            is Result.Success -> state.copy(
                                recipes = result.data,
                                isLoading = false,
                                error = null
                            )
                            is Result.Error -> state.copy(
                                isLoading = false,
                                error = result.message ?: "No se han podido cargar las recetas."
                            )
                            is Result.Loading -> state.copy(isLoading = true)
                        }
                    }
                }
        }
    }

    fun onQueryChanged(value: String) {
        _uiState.update { it.copy(query = value) }
        query.value = value
    }
}
