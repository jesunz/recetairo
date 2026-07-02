package com.jesunez.recetairo.feature.food.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jesunez.recetairo.feature.food.domain.model.FoodField
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.model.SaveResult
import com.jesunez.recetairo.feature.food.ui.AddFoodUiState
import com.jesunez.recetairo.feature.food.ui.viewmodel.AddFoodViewModel

import androidx.compose.ui.tooling.preview.Preview
import com.jesunez.recetairo.ui.theme.RecetairoTheme

@Composable
fun AddFoodScreen(
    onNavigateBack: () -> Unit,
    productInfo: ProductInfo? = null,
    viewModel: AddFoodViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // R15: pre-rellena el formulario cuando se navega desde el escáner de código de barras
    LaunchedEffect(productInfo) {
        productInfo?.let { viewModel.onProductInfoReceived(it) }
    }

    LaunchedEffect(state.saveResult) {
        if (state.saveResult is SaveResult.Success) {
            snackbarHostState.showSnackbar("Alimento añadido a la despensa")
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        AddFoodContent(
            state = state,
            onNameChanged = viewModel::onNameChanged,
            onQuantityChanged = viewModel::onQuantityChanged,
            onUnitChanged = viewModel::onUnitChanged,
            onCategoryChanged = viewModel::onCategoryChanged,
            onExpiryDateChanged = viewModel::onExpiryDateChanged,
            onSuggestionSelected = viewModel::onSuggestionSelected,
            onSaveClicked = viewModel::onSaveClicked,
            onCancelClicked = {
                viewModel.onCancelClicked()
                onNavigateBack()
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun AddFoodContent(
    state: AddFoodUiState,
    onNameChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onExpiryDateChanged: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Añadir alimento",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(4.dp))

        // Name field with autocomplete (R1, R2, R3, R4, R5)
        var showSuggestions by remember(state.nameSuggestions) {
            mutableStateOf(state.nameSuggestions.isNotEmpty())
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = { Text("Nombre *") },
                isError = FoodField.NAME in state.validationErrors,
                supportingText = if (FoodField.NAME in state.validationErrors) {
                    { Text(state.validationErrors.getValue(FoodField.NAME)) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Campo nombre obligatorio" },
                singleLine = true
            )
            DropdownMenu(
                expanded = showSuggestions,
                onDismissRequest = { showSuggestions = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                state.nameSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            showSuggestions = false
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Sugerencia: $suggestion"
                        }
                    )
                }
            }
        }

        // Quantity field (R1, R2, R6, R7)
        OutlinedTextField(
            value = state.quantity,
            onValueChange = onQuantityChanged,
            label = { Text("Cantidad *") },
            isError = FoodField.QUANTITY in state.validationErrors,
            supportingText = if (FoodField.QUANTITY in state.validationErrors) {
                { Text(state.validationErrors.getValue(FoodField.QUANTITY)) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo cantidad obligatorio" },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Unit field (R1)
        OutlinedTextField(
            value = state.unit,
            onValueChange = onUnitChanged,
            label = { Text("Unidad de medida") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo unidad de medida" },
            singleLine = true
        )

        // Category field (R1)
        OutlinedTextField(
            value = state.category,
            onValueChange = onCategoryChanged,
            label = { Text("Categoría") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo categoría" },
            singleLine = true
        )

        // Expiry date field (R1, R6, R7)
        OutlinedTextField(
            value = state.expiryDate,
            onValueChange = onExpiryDateChanged,
            label = { Text("Fecha de caducidad (DD/MM/AAAA)") },
            isError = FoodField.EXPIRY_DATE in state.validationErrors,
            supportingText = if (FoodField.EXPIRY_DATE in state.validationErrors) {
                { Text(state.validationErrors.getValue(FoodField.EXPIRY_DATE)) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Campo fecha de caducidad" },
            singleLine = true
        )

        // R10: inline error message on save failure
        if (state.saveResult is SaveResult.Failure) {
            Text(
                text = state.saveResult.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Error al guardar"
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // R8 loading indicator / R8 R11 action buttons
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = "Guardando alimento" }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClicked,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Cancelar" }
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onSaveClicked,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Guardar" }
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddFoodContentPreview() {
    RecetairoTheme {
        AddFoodContent(
            state = AddFoodUiState(
                name = "Leche",
                quantity = "2",
                unit = "litros",
                category = "Lácteos",
                expiryDate = "25/12/2025"
            ),
            onNameChanged = {},
            onQuantityChanged = {},
            onUnitChanged = {},
            onCategoryChanged = {},
            onExpiryDateChanged = {},
            onSuggestionSelected = {},
            onSaveClicked = {},
            onCancelClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddFoodContentErrorPreview() {
    RecetairoTheme {
        AddFoodContent(
            state = AddFoodUiState(
                validationErrors = mapOf(
                    FoodField.NAME to "El nombre es obligatorio",
                    FoodField.QUANTITY to "La cantidad es obligatoria"
                )
            ),
            onNameChanged = {},
            onQuantityChanged = {},
            onUnitChanged = {},
            onCategoryChanged = {},
            onExpiryDateChanged = {},
            onSuggestionSelected = {},
            onSaveClicked = {},
            onCancelClicked = {}
        )
    }
}
