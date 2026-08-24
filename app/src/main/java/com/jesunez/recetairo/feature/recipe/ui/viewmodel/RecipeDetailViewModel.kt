package com.jesunez.recetairo.feature.recipe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.GeneratedRecipesCache
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.usecase.ComputePantryMatchUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.GetRecipeByIdUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.RemoveRecipeUseCase
import com.jesunez.recetairo.feature.recipe.domain.usecase.SaveRecipeUseCase
import com.jesunez.recetairo.feature.recipe.ui.RecipeDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRecipeByIdUseCase: GetRecipeByIdUseCase,
    private val computePantryMatchUseCase: ComputePantryMatchUseCase,
    private val saveRecipeUseCase: SaveRecipeUseCase,
    private val removeRecipeUseCase: RemoveRecipeUseCase,
    private val generatedRecipesCache: GeneratedRecipesCache
) : ViewModel() {

    // Screen.RecipeDetail solo acepta "recipeId" hoy; "generatedIndex" anticipa la ruta extendida
    // que crea T25 (mismo patrón que GeneratedRecipesViewModel de T18 leyendo "ingredientNames"
    // antes de que Screen.GeneratedRecipes existiera). Exactamente uno de los dos llega no nulo.
    private val recipeId: Long? = savedStateHandle.get<Long>("recipeId")
    private val generatedIndex: Int? = savedStateHandle.get<Int>("generatedIndex")

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    init {
        when {
            recipeId != null -> loadSavedRecipe(recipeId)
            generatedIndex != null -> loadGeneratedRecipe(generatedIndex)
            else -> _uiState.update { it.copy(isLoading = false, notFound = true) }
        }
    }

    // R21/R22: carga única (no reactiva, a diferencia de FoodDetailViewModel.collectFood) — el
    // propio icono de favorito de esta pantalla (R24) puede eliminar la fila; seguir el Flow de
    // Room reflejaría ese borrado como "no encontrada" en vez de mantener la Receta visible con el
    // icono ya actualizado. onFavoriteClicked gestiona isSaved/recipe.id localmente después.
    private fun loadSavedRecipe(id: Long) {
        viewModelScope.launch {
            when (val result = getRecipeByIdUseCase(id).first()) {
                is Result.Success -> {
                    val recipe = result.data
                    if (recipe != null) {
                        _uiState.update {
                            it.copy(recipe = recipe, isSaved = true, isLoading = false, notFound = false)
                        }
                        computeMatch(recipe)
                    } else {
                        _uiState.update { it.copy(isLoading = false, notFound = true) }
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, notFound = true) }
                is Result.Loading -> Unit
            }
        }
    }

    // R21/R22: Receta recién generada, aún no persistida (R16) — se resuelve por posición en la
    // última tanda publicada por GeneratedRecipesViewModel (T18)
    private fun loadGeneratedRecipe(index: Int) {
        val recipe = generatedRecipesCache.recipes.value.getOrNull(index)
        if (recipe != null) {
            _uiState.update {
                it.copy(recipe = recipe, isSaved = false, isLoading = false, notFound = false)
            }
            computeMatch(recipe)
        } else {
            _uiState.update { it.copy(isLoading = false, notFound = true) }
        }
    }

    // R22/R23: Match_Despensa, para Recetas guardadas y recién generadas por igual
    private fun computeMatch(recipe: Recipe) {
        viewModelScope.launch {
            val match = computePantryMatchUseCase(recipe)
            _uiState.update { it.copy(match = match) }
        }
    }

    // R24: activa/desactiva el icono de guardado/favorito; persiste o elimina en Room y actualiza
    // el icono al momento, sin navegar fuera de Detalle_Receta
    fun onFavoriteClicked() {
        val recipe = _uiState.value.recipe ?: return
        viewModelScope.launch {
            if (_uiState.value.isSaved) {
                when (removeRecipeUseCase(recipe.id)) {
                    is Result.Success -> {
                        val updated = recipe.copy(id = 0)
                        _uiState.update { it.copy(isSaved = false, recipe = updated, error = null) }
                        computeMatch(updated)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(error = "No se ha podido quitar la receta de guardadas.")
                    }
                    is Result.Loading -> Unit
                }
            } else {
                when (val result = saveRecipeUseCase(recipe)) {
                    is Result.Success -> {
                        val updated = recipe.copy(id = result.data)
                        _uiState.update { it.copy(isSaved = true, recipe = updated, error = null) }
                        computeMatch(updated)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(error = "No se ha podido guardar la receta.")
                    }
                    is Result.Loading -> Unit
                }
            }
        }
    }
}
