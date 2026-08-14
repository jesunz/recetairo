package com.jesunez.recetairo.feature.food.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jesunez.recetairo.core.ui.component.ConfirmationDialog
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.ui.FoodDetailUiState
import com.jesunez.recetairo.feature.food.ui.viewmodel.FoodDetailViewModel
import com.jesunez.recetairo.ui.component.emoji
import com.jesunez.recetairo.ui.theme.RecetairoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FoodDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // R14: once the deletion succeeds, leave Vista_Detalle and return to Vista_Despensa
    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) onNavigateBack()
    }

    FoodDetailContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onDeleteClicked = viewModel::onDeleteClicked,
        onDeleteCancelled = viewModel::onDeleteCancelled,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        modifier = modifier
    )
}

@Composable
fun FoodDetailContent(
    state: FoodDetailUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClicked: () -> Unit = {},
    onDeleteCancelled: () -> Unit = {},
    onDeleteConfirmed: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics { contentDescription = "Volver" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Detalle del alimento",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = "Cargando alimento" }
                )
                // R15: the food no longer exists in the pantry (deleted from Modo_Seleccion elsewhere)
                state.notFound -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Este alimento ya no existe en tu despensa.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Este alimento ya no existe en tu despensa."
                        }
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Volver a la despensa")
                    }
                }
                state.food != null -> FoodDetailBody(
                    food = state.food,
                    onDeleteClicked = onDeleteClicked
                )
            }
        }
    }

    // R13/R14: shared ConfirmationDialog for the individual deletion from Vista_Detalle
    if (state.showDeleteConfirmation) {
        ConfirmationDialog(
            title = "Eliminar alimento",
            message = "¿Eliminar este alimento de la despensa? Esta acción no se puede deshacer.",
            confirmText = "Eliminar",
            cancelText = "Cancelar",
            onConfirm = onDeleteConfirmed,
            onCancel = onDeleteCancelled
        )
    }
}

@Composable
private fun FoodDetailBody(
    food: Food,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = food.emoji ?: FoodCategory.fromLabel(food.category).emoji(),
                fontSize = 48.sp
            )
        }

        Spacer(modifier = Modifier.padding(12.dp))

        // R12: name, quantity, unit, category, expiry date (or its absence), emoji
        Text(
            text = food.name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.semantics { contentDescription = "Nombre: ${food.name}" }
        )

        Spacer(modifier = Modifier.padding(16.dp))

        FoodDetailField(label = "Cantidad", value = "${food.quantity} ${food.unit}".trim())
        FoodDetailField(label = "Categoría", value = food.category.ifBlank { "Sin categoría" })
        FoodDetailField(
            label = "Caducidad",
            value = food.expiryDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Sin fecha de caducidad"
        )

        Spacer(modifier = Modifier.padding(16.dp))

        // R13: action to delete the shown food
        Button(
            onClick = onDeleteClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.semantics { contentDescription = "Eliminar alimento" }
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Eliminar")
        }
    }
}

@Composable
private fun FoodDetailField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { contentDescription = "$label: $value" }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun FoodDetailContentPreview() {
    RecetairoTheme {
        FoodDetailContent(
            state = FoodDetailUiState(
                food = Food(
                    id = 1,
                    name = "Leche Entera",
                    quantity = 1.0,
                    unit = "L",
                    category = "Lácteos",
                    expiryDate = LocalDate.now().plusDays(5)
                ),
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun FoodDetailContentNotFoundPreview() {
    RecetairoTheme {
        FoodDetailContent(
            state = FoodDetailUiState(isLoading = false, notFound = true),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun FoodDetailContentDeleteConfirmationPreview() {
    RecetairoTheme {
        FoodDetailContent(
            state = FoodDetailUiState(
                food = Food(id = 1, name = "Leche Entera", quantity = 1.0, unit = "L", category = "Lácteos"),
                isLoading = false,
                showDeleteConfirmation = true
            ),
            onNavigateBack = {}
        )
    }
}
