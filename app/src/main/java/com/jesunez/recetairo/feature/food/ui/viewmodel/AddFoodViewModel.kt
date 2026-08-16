package com.jesunez.recetairo.feature.food.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.FoodField
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.model.SaveResult
import com.jesunez.recetairo.feature.food.domain.usecase.InsertFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.SearchFoodHistoryUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.ValidateFoodUseCase
import com.jesunez.recetairo.feature.food.ui.AddFoodUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val validateFoodUseCase: ValidateFoodUseCase,
    private val insertFoodUseCase: InsertFoodUseCase,
    private val searchFoodHistoryUseCase: SearchFoodHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    private val nameQuery = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        collectNameSuggestions()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun collectNameSuggestions() {
        viewModelScope.launch {
            nameQuery
                .debounce(AUTOCOMPLETE_DEBOUNCE_MS.milliseconds)
                .flatMapLatest { query ->
                    if (query.isEmpty()) flowOf(emptyList())
                    else searchFoodHistoryUseCase(query)
                }
                .collect { suggestions ->
                    // searchFoodHistoryUseCase is backed by a reactive Room Flow: inserting the
                    // food being saved invalidates it and it re-emits with the just-saved name
                    // matching itself, reopening the dropdown while the screen navigates away.
                    // Once save has succeeded there is nothing left to suggest against, so ignore it.
                    if (_uiState.value.saveResult != SaveResult.Success) {
                        _uiState.update { it.copy(nameSuggestions = suggestions) }
                    }
                }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, validationErrors = it.validationErrors - FoodField.NAME) }
        nameQuery.tryEmit(value)
    }

    fun onQuantityChanged(value: String) {
        _uiState.update { it.copy(quantity = value, validationErrors = it.validationErrors - FoodField.QUANTITY) }
    }

    fun onUnitChanged(value: String) {
        _uiState.update { it.copy(unit = value, validationErrors = it.validationErrors - FoodField.UNIT) }
    }

    fun onCategoryChanged(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun onExpiryDateChanged(value: String) {
        _uiState.update { it.copy(expiryDate = value, validationErrors = it.validationErrors - FoodField.EXPIRY_DATE) }
    }

    fun onSuggestionSelected(suggestion: String) {
        _uiState.update { it.copy(name = suggestion, nameSuggestions = emptyList()) }
    }

    fun onProductInfoReceived(productInfo: ProductInfo) {
        // R15: pre-rellena solo los campos con dato recibido; el resto permanece editable
        _uiState.update {
            it.copy(
                name = productInfo.name ?: it.name,
                brand = productInfo.brand ?: it.brand,
                category = productInfo.category ?: it.category,
                imageUrl = productInfo.imageUrl ?: it.imageUrl
            )
        }
    }

    fun onSaveClicked() {
        // Dismiss any pending name suggestion so it doesn't linger while the screen navigates away
        _uiState.update { it.copy(nameSuggestions = emptyList()) }

        val state = _uiState.value
        val parseErrors = mutableMapOf<FoodField, String>()

        val quantity = state.quantity.replace(',', '.').toDoubleOrNull()
        if (quantity == null) {
            parseErrors[FoodField.QUANTITY] = "La cantidad debe ser un número válido"
        }

        val expiryDate: LocalDate? = when {
            state.expiryDate.isBlank() -> null
            else -> try {
                LocalDate.parse(state.expiryDate, DATE_FORMATTER)
            } catch (_: DateTimeParseException) {
                parseErrors[FoodField.EXPIRY_DATE] = "La fecha debe tener el formato DD/MM/AAAA"
                null
            }
        }

        if (parseErrors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = parseErrors) }
            return
        }

        val food = Food(
            name = state.name,
            quantity = quantity!!,
            unit = state.unit,
            // Normalize to a canonical FoodCategory label so it always matches the
            // exact-string filter used by getFoodsByCategory, even if the user never
            // touched the dropdown (default) or it came from a raw barcode-lookup string
            category = FoodCategory.fromLabel(state.category).label(),
            expiryDate = expiryDate
        )

        val validationResult = validateFoodUseCase(food)
        if (!validationResult.isValid) {
            _uiState.update { it.copy(validationErrors = validationResult.errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, saveResult = null) }
            val result = insertFoodUseCase(food)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    saveResult = when (result) {
                        is Result.Success -> SaveResult.Success
                        is Result.Error -> SaveResult.Failure(
                            result.message ?: "No se ha podido guardar el alimento. Inténtalo de nuevo."
                        )
                        is Result.Loading -> null
                    }
                )
            }
        }
    }

    fun onCancelClicked() {
        _uiState.update { AddFoodUiState() }
    }

    private companion object {
        const val AUTOCOMPLETE_DEBOUNCE_MS = 300L
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
