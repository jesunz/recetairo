package com.jesunez.recetairo.feature.recipe.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.recipe.ui.IngredientSelectionUiState
import com.jesunez.recetairo.feature.recipe.ui.component.IngredientCategorySection
import com.jesunez.recetairo.feature.recipe.ui.component.ServingsSelector
import com.jesunez.recetairo.feature.recipe.ui.viewmodel.IngredientSelectionViewModel
import com.jesunez.recetairo.ui.theme.RecetairoTheme

@Composable
fun IngredientSelectionScreen(
    onNavigateBack: () -> Unit,
    onGenerateRecipeClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IngredientSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    IngredientSelectionContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onCategoryToggled = viewModel::onCategoryToggled,
        onIngredientToggled = viewModel::onIngredientToggled,
        onServingsSelected = viewModel::onServingsSelected,
        onGenerateRecipeClick = { onGenerateRecipeClick(state.selectedNames.toList(), state.servings) },
        modifier = modifier
    )
}

@Composable
fun IngredientSelectionContent(
    state: IngredientSelectionUiState,
    onNavigateBack: () -> Unit,
    onCategoryToggled: (FoodCategory) -> Unit,
    onIngredientToggled: (String) -> Unit,
    onServingsSelected: (Int) -> Unit,
    onGenerateRecipeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics { contentDescription = "Volver" }
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Selecciona Ingredientes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                // R8: counter reflecting the current selection, out of the 3-ingredient max
                Text(
                    text = "Seleccionados (${state.selectedNames.size}/3)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                // R9: disabled while no ingredient is selected
                Button(
                    onClick = onGenerateRecipeClick,
                    enabled = state.isGenerateEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Generar Receta" }
                ) {
                    Text("Generar Receta")
                }
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
                    modifier = Modifier.semantics { contentDescription = "Cargando despensa" }
                )

                state.error != null -> Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .semantics { contentDescription = "Error: ${state.error}" }
                )

                // R12: explicit empty state, "Generar Receta" stays disabled (isGenerateEnabled
                // is already false with no Foods to select from)
                state.categorized.isEmpty() -> {
                    val emptyMessage = "Tu despensa está vacía. Añade alimentos para poder " +
                        "generar recetas."
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

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    // R1/R2: servings selector between the header and the ingredient sections,
                    // visible whenever the pantry has content
                    ServingsSelector(
                        servings = state.servings,
                        onServingsSelected = onServingsSelected,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Selected ingredients stay visible even if their category is collapsed
                    if (state.selectedNames.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.selectedNames.forEach { name ->
                                FilterChip(
                                    selected = true,
                                    onClick = { onIngredientToggled(name) },
                                    label = { Text(name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.semantics {
                                        contentDescription = "$name, seleccionado"
                                    }
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.categorized.entries.toList(), key = { it.key.name }) { (category, foods) ->
                            IngredientCategorySection(
                                category = category,
                                foods = foods,
                                isExpanded = category == state.expandedCategory,
                                selectedNames = state.selectedNames,
                                onCategoryToggled = { onCategoryToggled(category) },
                                onIngredientToggled = onIngredientToggled
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun IngredientSelectionContentPreview() {
    RecetairoTheme {
        IngredientSelectionContent(
            state = IngredientSelectionUiState(
                categorized = mapOf(
                    FoodCategory.VERDURAS to listOf(
                        Food(id = 1, name = "Tomate", quantity = 3.0, unit = "u", category = "Verduras"),
                        Food(id = 2, name = "Cebolla", quantity = 2.0, unit = "u", category = "Verduras")
                    ),
                    FoodCategory.LACTEOS to listOf(
                        Food(id = 3, name = "Leche Entera", quantity = 1.0, unit = "L", category = "Lácteos")
                    )
                ),
                expandedCategory = FoodCategory.VERDURAS,
                selectedNames = setOf("Tomate"),
                isLoading = false
            ),
            onNavigateBack = {},
            onCategoryToggled = {},
            onIngredientToggled = {},
            onServingsSelected = {},
            onGenerateRecipeClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun IngredientSelectionContentEmptyPreview() {
    RecetairoTheme {
        IngredientSelectionContent(
            state = IngredientSelectionUiState(categorized = emptyMap(), isLoading = false),
            onNavigateBack = {},
            onCategoryToggled = {},
            onIngredientToggled = {},
            onServingsSelected = {},
            onGenerateRecipeClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun IngredientSelectionContentLoadingPreview() {
    RecetairoTheme {
        IngredientSelectionContent(
            state = IngredientSelectionUiState(isLoading = true),
            onNavigateBack = {},
            onCategoryToggled = {},
            onIngredientToggled = {},
            onServingsSelected = {},
            onGenerateRecipeClick = {}
        )
    }
}
