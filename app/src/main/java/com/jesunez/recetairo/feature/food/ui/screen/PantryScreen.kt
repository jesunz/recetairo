package com.jesunez.recetairo.feature.food.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.PantryFilter
import com.jesunez.recetairo.feature.food.ui.PantryUiState
import com.jesunez.recetairo.feature.food.ui.viewmodel.PantryViewModel
import com.jesunez.recetairo.ui.component.emoji
import com.jesunez.recetairo.ui.theme.RecetairoTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun PantryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    PantryContent(
        state = state,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun PantryContent(
    state: PantryUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PantryHeader(
                title = state.filter.title(),
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                // R8: loading indicator while the pantry query is in flight
                state.isLoading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = "Cargando despensa" }
                )
                // R9: descriptive error instead of a silently empty list
                state.error != null -> Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .semantics { contentDescription = "Error: ${state.error}" }
                )
                // R10: explicit empty state instead of a blank list
                state.foods.isEmpty() -> {
                    val emptyMessage = state.filter.emptyMessage()
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(24.dp)
                            .semantics { contentDescription = emptyMessage }
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.foods, key = { it.id }) { food ->
                        PantryFoodRow(food = food)
                    }
                }
            }
        }
    }
}

@Composable
private fun PantryHeader(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun PantryFilter.title(): String = when (this) {
    PantryFilter.All -> "Despensa"
    is PantryFilter.ByCategory -> category.label()
    PantryFilter.ExpiringSoon -> "Próximos a vencer"
}

private fun PantryFilter.emptyMessage(): String = when (this) {
    PantryFilter.All -> "Tu despensa está vacía. Añade alimentos para empezar."
    is PantryFilter.ByCategory -> "No hay alimentos en la categoría ${category.label()}."
    PantryFilter.ExpiringSoon -> "No hay alimentos próximos a vencer."
}

@Composable
private fun PantryFoodRow(
    food: Food,
    modifier: Modifier = Modifier
) {
    // R11: distinguish already-expired foods from those expiring soon; foods without an
    // expiryDate (e.g. non-perishables) show no expiry indicator and use a neutral tint.
    val daysLeft = food.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val isExpired = daysLeft != null && daysLeft < 0
    val expiryText = when {
        daysLeft == null -> null
        daysLeft < 0 -> "Caducado"
        daysLeft == 0L -> "Vence hoy"
        daysLeft == 1L -> "Vence mañana"
        else -> "Vence en $daysLeft días"
    }
    val accentColor = when {
        isExpired -> MaterialTheme.colorScheme.error
        daysLeft != null -> MaterialTheme.colorScheme.tertiary
        else -> null
    }
    val quantityText = "${food.quantity} ${food.unit}".trim()
    val description = listOfNotNull(food.name, quantityText, expiryText).joinToString(", ")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = CircleShape, // Pill shape, matching ExpiringFoodCard's "digital organic" identity
        colors = CardDefaults.cardColors(
            containerColor = accentColor?.copy(alpha = 0.12f)
                ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular container for emoji, matching CategoryGrid/ExpiringSoonSection
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = food.emoji ?: FoodCategory.fromLabel(food.category).emoji(),
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = accentColor ?: Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = quantityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (expiryText != null && accentColor != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = expiryText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun PantryContentPreview() {
    RecetairoTheme {
        PantryContent(
            state = PantryUiState(
                filter = PantryFilter.All,
                foods = listOf(
                    Food(id = 1, name = "Leche Entera", quantity = 1.0, unit = "L", category = "Lácteos", expiryDate = LocalDate.now()),
                    Food(id = 2, name = "Aguacate", quantity = 2.0, unit = "u", category = "Frutas", expiryDate = LocalDate.now().minusDays(1)),
                    Food(id = 3, name = "Arroz", quantity = 1.0, unit = "kg", category = "Cereales")
                ),
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun PantryContentEmptyPreview() {
    RecetairoTheme {
        PantryContent(
            state = PantryUiState(
                filter = PantryFilter.ExpiringSoon,
                foods = emptyList(),
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}
