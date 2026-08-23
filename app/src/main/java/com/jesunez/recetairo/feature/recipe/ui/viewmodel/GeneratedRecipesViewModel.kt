package com.jesunez.recetairo.feature.recipe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.GeneratedRecipesCache
import com.jesunez.recetairo.feature.recipe.domain.usecase.GenerateRecipesUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.SaveRecipesUseCase
import com.jesunez.recetairo.feature.recipe.ui.GeneratedRecipesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GeneratedRecipesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generateRecipesUseCase: GenerateRecipesUseCase,
    private val saveRecipesUseCase: SaveRecipesUseCase,
    private val generatedRecipesCache: GeneratedRecipesCache
) : ViewModel() {

    private val ingredientNames: List<String> = savedStateHandle.get<String>("ingredientNames")
        ?.split(",")
        ?.filter { it.isNotBlank() }
        .orEmpty()

    private val _uiState = MutableStateFlow(GeneratedRecipesUiState())
    val uiState: StateFlow<GeneratedRecipesUiState> = _uiState.asStateFlow()

    init {
        generateRecipes()
    }

    private fun generateRecipes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = generateRecipesUseCase(ingredientNames)) {
                is Result.Success -> {
                    // R16/R18: kept in memory only until the user explicitly saves. Published here,
                    // right after a successful generation rather than at save time, so a tap on any
                    // of the 3 cards can already resolve via generatedIndex in Detalle_Receta before
                    // anything is saved.
                    generatedRecipesCache.publish(result.data)
                    _uiState.update {
                        it.copy(recipes = result.data, checked = emptySet(), isLoading = false, error = null)
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.message ?: "No se han podido generar recetas. Inténtalo de nuevo."
                    )
                }
                is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun onRetryClicked() {
        generateRecipes()
    }

    fun onRecipeCheckedChanged(index: Int, checked: Boolean) {
        _uiState.update { state ->
            state.copy(checked = if (checked) state.checked + index else state.checked - index)
        }
    }

    fun onSaveSelectedClicked() {
        val state = _uiState.value
        val selectedRecipes = state.recipes.filterIndexed { index, _ -> index in state.checked }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = saveRecipesUseCase(selectedRecipes)) {
                is Result.Success -> _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = result.message ?: "No se han podido guardar las recetas seleccionadas."
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }
}
