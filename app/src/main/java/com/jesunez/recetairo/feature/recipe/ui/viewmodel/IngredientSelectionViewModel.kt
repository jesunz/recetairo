package com.jesunez.recetairo.feature.recipe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.usecase.GetAllFoodsUseCase
import com.jesunez.recetairo.feature.recipe.ui.IngredientSelectionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IngredientSelectionViewModel @Inject constructor(
    private val getAllFoodsUseCase: GetAllFoodsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngredientSelectionUiState())
    val uiState: StateFlow<IngredientSelectionUiState> = _uiState.asStateFlow()

    init {
        collectPantry()
    }

    private fun collectPantry() {
        viewModelScope.launch {
            getAllFoodsUseCase().collect { result ->
                _uiState.update { state ->
                    when (result) {
                        is Result.Success -> {
                            val categorized = result.data.groupByCategory()
                            state.copy(
                                categorized = categorized,
                                // R6: first non-empty category starts expanded, the rest collapsed.
                                // Only set on the first successful load: a later re-emission (e.g.
                                // the pantry changed while this screen is open) must not undo an
                                // accordion section the user already expanded by hand.
                                expandedCategory = state.expandedCategory ?: categorized.keys.firstOrNull(),
                                isLoading = false,
                                error = null
                            )
                        }
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

    // R6: groups by FoodCategory (tolerant fromLabel, same pattern as GetCategorySummariesUseCase),
    // omitting categories with no items; FoodCategory.entries order keeps the grouping deterministic
    // so "first non-empty category" above is well-defined.
    private fun List<Food>.groupByCategory(): Map<FoodCategory, List<Food>> =
        FoodCategory.entries
            .associateWith { category -> filter { FoodCategory.fromLabel(it.category) == category } }
            .filterValues { it.isNotEmpty() }

    fun onCategoryToggled(category: FoodCategory) {
        _uiState.update { state ->
            state.copy(expandedCategory = if (state.expandedCategory == category) null else category)
        }
    }

    fun onIngredientToggled(name: String) {
        _uiState.update { state ->
            val selected = state.selectedNames
            val updatedSelection = when {
                name in selected -> selected - name
                // R7: reject a 4th selection without modifying the 3 already chosen
                selected.size >= MAX_SELECTED_INGREDIENTS -> selected
                else -> selected + name
            }
            state.copy(selectedNames = updatedSelection)
        }
    }

    // R3: changing servings never touches selectedNames, both are independent fields of the
    // same state. coerceIn is a cheap defensive clamp; the UI (T4) only ever sends 1..4.
    fun onServingsSelected(servings: Int) {
        _uiState.update { state -> state.copy(servings = servings.coerceIn(MIN_SERVINGS, MAX_SERVINGS)) }
    }

    private companion object {
        const val MAX_SELECTED_INGREDIENTS = 3
        const val MIN_SERVINGS = 1
        const val MAX_SERVINGS = 4
    }
}
