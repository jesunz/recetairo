package com.jesunez.recetairo.feature.recipe.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jesunez.recetairo.core.ui.component.RecipeCard
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import com.jesunez.recetairo.feature.recipe.ui.RecipeListUiState
import com.jesunez.recetairo.feature.recipe.ui.viewmodel.RecipeListViewModel
import com.jesunez.recetairo.ui.theme.RecetairoTheme

@Composable
fun RecipeListScreen(
    onGenerateClick: () -> Unit,
    onRecipeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    RecipeListContent(
        state = state,
        onQueryChanged = viewModel::onQueryChanged,
        onGenerateClick = onGenerateClick,
        onRecipeClick = onRecipeClick,
        modifier = modifier
    )
}

@Composable
fun RecipeListContent(
    state: RecipeListUiState,
    onQueryChanged: (String) -> Unit,
    onGenerateClick: () -> Unit,
    onRecipeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Text(
                text = "Recetas",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // R2: live search field, filters Recetas_Guardadas by title as the user types
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                placeholder = { Text("Buscar recetas...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Buscar recetas por título" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // R3: CTA that navigates to Selector_Ingredientes
            GenerateWithAiCard(onClick = onGenerateClick)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Guardadas Recientemente",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .semantics { contentDescription = "Cargando recetas" }
                    )

                    state.error != null -> Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .semantics { contentDescription = "Error: ${state.error}" }
                    )

                    // R4: explicit empty state instead of a blank list, either because the
                    // pantry has no saved recipes at all or because the search has no matches
                    state.recipes.isEmpty() -> {
                        val emptyMessage = if (state.query.isBlank()) {
                            "Aún no tienes recetas guardadas. Genera tu primera receta con IA."
                        } else {
                            "No se encontraron recetas para \"${state.query}\"."
                        }
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .semantics { contentDescription = emptyMessage }
                        )
                    }

                    else -> RecipeGrid(recipes = state.recipes, onRecipeClick = onRecipeClick)
                }
            }

            // Padding to ensure content is not hidden behind the floating bottom bar
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun GenerateWithAiCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Generar con IA. Descubre nuevas recetas a partir de tu despensa"
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = "Generar con IA",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Descubre nuevas recetas con tu despensa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun RecipeGrid(
    recipes: List<Recipe>,
    onRecipeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        recipes.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeListContentPreview() {
    RecetairoTheme {
        RecipeListContent(
            state = RecipeListUiState(
                recipes = listOf(
                    Recipe(
                        id = 1,
                        title = "Tortilla de Patatas",
                        difficulty = RecipeDifficulty.FACIL,
                        durationMinutes = 30,
                        servings = 4,
                        ingredients = listOf(RecipeIngredient(name = "Patata", quantityText = "4 u")),
                        steps = listOf("Pelar y cortar las patatas.")
                    ),
                    Recipe(
                        id = 2,
                        title = "Ensalada César con Pollo a la Plancha",
                        difficulty = RecipeDifficulty.MEDIA,
                        durationMinutes = 20,
                        servings = 2,
                        ingredients = listOf(RecipeIngredient(name = "Lechuga", quantityText = "1 u")),
                        steps = listOf("Lavar la lechuga.")
                    ),
                    Recipe(
                        id = 3,
                        title = "Lentejas Estofadas",
                        difficulty = RecipeDifficulty.DIFICIL,
                        durationMinutes = 60,
                        servings = 6,
                        ingredients = listOf(RecipeIngredient(name = "Lentejas", quantityText = "500 g")),
                        steps = listOf("Poner las lentejas en remojo.")
                    )
                ),
                isLoading = false
            ),
            onQueryChanged = {},
            onGenerateClick = {},
            onRecipeClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeListContentEmptyPreview() {
    RecetairoTheme {
        RecipeListContent(
            state = RecipeListUiState(recipes = emptyList(), isLoading = false),
            onQueryChanged = {},
            onGenerateClick = {},
            onRecipeClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeListContentLoadingPreview() {
    RecetairoTheme {
        RecipeListContent(
            state = RecipeListUiState(isLoading = true),
            onQueryChanged = {},
            onGenerateClick = {},
            onRecipeClick = {}
        )
    }
}
