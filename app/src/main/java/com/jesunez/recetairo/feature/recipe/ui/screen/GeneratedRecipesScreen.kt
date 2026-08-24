package com.jesunez.recetairo.feature.recipe.ui.screen

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jesunez.recetairo.core.ui.component.RecipeCard
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import com.jesunez.recetairo.feature.recipe.ui.GeneratedRecipesUiState
import com.jesunez.recetairo.feature.recipe.ui.viewmodel.GeneratedRecipesViewModel
import com.jesunez.recetairo.ui.theme.RecetairoTheme

@Composable
fun GeneratedRecipesScreen(
    onNavigateBack: () -> Unit,
    onRecipeClick: (Int) -> Unit,
    onSavedSuccessfully: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeneratedRecipesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // R20: navigates back to Menu_Recetas once the selected recipes are persisted. Observed as a
    // state flag (same pattern as AddFoodUiState.saveResult/FoodDetailUiState.navigateBack)
    // instead of navigating directly from the ViewModel.
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSavedSuccessfully()
    }

    GeneratedRecipesContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onRecipeClick = onRecipeClick,
        onRecipeCheckedChanged = viewModel::onRecipeCheckedChanged,
        onRetryClicked = viewModel::onRetryClicked,
        onSaveSelectedClicked = viewModel::onSaveSelectedClicked,
        modifier = modifier
    )
}

@Composable
fun GeneratedRecipesContent(
    state: GeneratedRecipesUiState,
    onNavigateBack: () -> Unit,
    onRecipeClick: (Int) -> Unit,
    onRecipeCheckedChanged: (Int, Boolean) -> Unit,
    onRetryClicked: () -> Unit,
    onSaveSelectedClicked: () -> Unit,
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
                    text = "Recetas Generadas",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        },
        bottomBar = {
            // R19: enabled only once at least one card is checked; disabled while a previous save
            // is still in flight (isSaving) so a double tap cannot fire SaveRecipesUseCase twice.
            if (state.recipes.isNotEmpty() && state.error == null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onSaveSelectedClicked,
                        enabled = state.checked.isNotEmpty() && !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Guardar seleccionadas" }
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Guardar seleccionadas")
                    }
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
                // R10: loading state shown while GenerateRecipesUseCase is in flight
                state.isLoading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = "Generando recetas" }
                )

                // R11: descriptive error with a retry action; no recipe is assumed generated
                state.error != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { contentDescription = "Error: ${state.error}" }
                    )
                    Spacer(modifier = Modifier.padding(top = 16.dp))
                    OutlinedButton(
                        onClick = onRetryClicked,
                        modifier = Modifier.semantics { contentDescription = "Reintentar" }
                    ) {
                        Text("Reintentar")
                    }
                }

                // R17: the 3 generated recipes as cards, each with a selection checkbox (R19).
                // R18: tapping a card outside the checkbox navigates to Detalle_Receta by index,
                // matching the position GeneratedRecipesCache was published with.
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(state.recipes) { index, recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(index) },
                            checked = index in state.checked,
                            onCheckedChange = { checked -> onRecipeCheckedChanged(index, checked) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun GeneratedRecipesContentPreview() {
    RecetairoTheme {
        GeneratedRecipesContent(
            state = GeneratedRecipesUiState(
                recipes = listOf(
                    Recipe(
                        title = "Tortilla de Patatas",
                        difficulty = RecipeDifficulty.FACIL,
                        durationMinutes = 30,
                        servings = 4,
                        ingredients = listOf(RecipeIngredient(name = "Patata", quantityText = "4 u")),
                        steps = listOf("Pelar y cortar las patatas.")
                    ),
                    Recipe(
                        title = "Ensalada César con Pollo a la Plancha",
                        difficulty = RecipeDifficulty.MEDIA,
                        durationMinutes = 20,
                        servings = 2,
                        ingredients = listOf(RecipeIngredient(name = "Lechuga", quantityText = "1 u")),
                        steps = listOf("Lavar la lechuga.")
                    ),
                    Recipe(
                        title = "Lentejas Estofadas",
                        difficulty = RecipeDifficulty.DIFICIL,
                        durationMinutes = 60,
                        servings = 6,
                        ingredients = listOf(RecipeIngredient(name = "Lentejas", quantityText = "500 g")),
                        steps = listOf("Poner las lentejas en remojo.")
                    )
                ),
                checked = setOf(0),
                isLoading = false
            ),
            onNavigateBack = {},
            onRecipeClick = {},
            onRecipeCheckedChanged = { _, _ -> },
            onRetryClicked = {},
            onSaveSelectedClicked = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun GeneratedRecipesContentLoadingPreview() {
    RecetairoTheme {
        GeneratedRecipesContent(
            state = GeneratedRecipesUiState(isLoading = true),
            onNavigateBack = {},
            onRecipeClick = {},
            onRecipeCheckedChanged = { _, _ -> },
            onRetryClicked = {},
            onSaveSelectedClicked = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun GeneratedRecipesContentErrorPreview() {
    RecetairoTheme {
        GeneratedRecipesContent(
            state = GeneratedRecipesUiState(
                isLoading = false,
                error = "No se han podido generar recetas. Inténtalo de nuevo."
            ),
            onNavigateBack = {},
            onRecipeClick = {},
            onRecipeCheckedChanged = { _, _ -> },
            onRetryClicked = {},
            onSaveSelectedClicked = {}
        )
    }
}
