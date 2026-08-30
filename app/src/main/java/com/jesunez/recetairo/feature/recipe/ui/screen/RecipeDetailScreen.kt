package com.jesunez.recetairo.feature.recipe.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jesunez.recetairo.core.ui.component.ConfirmationDialog
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredientMatch
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeMatch
import com.jesunez.recetairo.feature.recipe.ui.RecipeDetailUiState
import com.jesunez.recetairo.feature.recipe.ui.viewmodel.RecipeDetailViewModel
import com.jesunez.recetairo.ui.theme.RecetairoTheme

@Composable
fun RecipeDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // R24: unmarking a saved Receta removes it from Recetas_Guardadas; warn before doing so
    // instead of removing it on the first tap.
    var showUnfavoriteConfirmation by remember { mutableStateOf(false) }
    if (showUnfavoriteConfirmation) {
        ConfirmationDialog(
            title = "¿Quitar de recetas guardadas?",
            message = "Se eliminará esta receta de tus recetas guardadas.",
            confirmText = "Quitar",
            cancelText = "Cancelar",
            onConfirm = {
                showUnfavoriteConfirmation = false
                viewModel.onFavoriteClicked()
            },
            onCancel = { showUnfavoriteConfirmation = false }
        )
    }

    RecipeDetailContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onFavoriteClicked = {
            if (state.isSaved) {
                showUnfavoriteConfirmation = true
            } else {
                viewModel.onFavoriteClicked()
            }
        },
        // R25: opens Android's share sheet with a text summary of the Receta (title, ingredients,
        // steps) — shareText is already fully formatted by RecipeDetailUiState.
        onShareClicked = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, state.shareText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir receta"))
        },
        modifier = modifier
    )
}

@Composable
fun RecipeDetailContent(
    state: RecipeDetailUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteClicked: () -> Unit = {},
    onShareClicked: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
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
                    text = "Detalle de la Receta",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.weight(1f)
                )
                if (state.recipe != null) {
                    // R25: opens the Android share sheet with a text summary of the Receta
                    IconButton(
                        onClick = onShareClicked,
                        modifier = Modifier.semantics { contentDescription = "Compartir receta" }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    }
                    // R24: reflects whether the shown Receta is currently in Recetas_Guardadas;
                    // toggling persists/removes it in Room and updates the icon immediately.
                    IconButton(
                        onClick = onFavoriteClicked,
                        modifier = Modifier.semantics {
                            contentDescription = if (state.isSaved) {
                                "Quitar de recetas guardadas"
                            } else {
                                "Guardar receta"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (state.isSaved) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
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
                state.isLoading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = "Cargando receta" }
                )

                state.notFound -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Esta receta ya no está disponible.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Esta receta ya no está disponible."
                        }
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Volver")
                    }
                }

                state.recipe != null -> RecipeDetailBody(
                    recipe = state.recipe,
                    match = state.match,
                    error = state.error
                )
            }
        }
    }
}

@Composable
private fun RecipeDetailBody(
    recipe: Recipe,
    match: RecipeMatch?,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // R21: title, duration, difficulty, servings
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Título: ${recipe.title}" }
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "${recipe.durationMinutes} minutos, " +
                    "dificultad ${recipe.difficulty.label()}, ${recipe.servings} raciones"
            }
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${recipe.durationMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = recipe.difficulty.label(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${recipe.servings} raciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.padding(12.dp))

        // R22/R23: Match_Despensa badge, recalculated every time Detalle_Receta is opened
        if (match != null) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.semantics {
                    contentDescription = "${match.percentage} por ciento de coincidencia con tu despensa"
                }
            ) {
                Text(
                    text = "${match.percentage}% coincide con tu despensa",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.padding(12.dp))
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .semantics { contentDescription = "Error: $error" }
            )
        }

        // R21/R23: ingredient list, visually distinguishing owned from missing (Match_Despensa)
        RecipeDetailSection(title = "Ingredientes") {
            val ingredientRows = match?.ingredients?.map { it.ingredient to it.owned }
                ?: recipe.ingredients.map { it to null }
            ingredientRows.forEach { (ingredient, owned) ->
                IngredientMatchRow(ingredient = ingredient, owned = owned)
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        // R21: ordered preparation steps
        RecipeDetailSection(title = "Preparación") {
            recipe.steps.forEachIndexed { index, step ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics {
                            contentDescription = "Paso ${index + 1}: $step"
                        }
                    )
                }
            }
        }

    }
}

@Composable
private fun RecipeDetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        content()
        Spacer(modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun IngredientMatchRow(
    ingredient: RecipeIngredient,
    // null when there is no Match_Despensa yet (match still loading) — rendered neutrally
    owned: Boolean?,
    modifier: Modifier = Modifier
) {
    val statusDescription = when (owned) {
        true -> "poseído"
        false -> "falta"
        null -> ""
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${ingredient.name}, ${ingredient.quantityText}" +
                    if (statusDescription.isNotEmpty()) ", $statusDescription" else ""
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (owned == true) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (owned == true) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (owned == false) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ingredient.quantityText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeDetailContentPreview() {
    RecetairoTheme {
        RecipeDetailContent(
            state = RecipeDetailUiState(
                recipe = Recipe(
                    id = 1,
                    title = "Tortilla de Patatas",
                    difficulty = RecipeDifficulty.FACIL,
                    durationMinutes = 30,
                    servings = 4,
                    ingredients = listOf(
                        RecipeIngredient(name = "Patata", quantityText = "4 u"),
                        RecipeIngredient(name = "Huevo", quantityText = "6 u"),
                        RecipeIngredient(name = "Cebolla", quantityText = "1 u")
                    ),
                    steps = listOf(
                        "Pelar y cortar las patatas.",
                        "Freír las patatas y la cebolla.",
                        "Batir los huevos y mezclar con las patatas.",
                        "Cuajar la tortilla en la sartén."
                    )
                ),
                match = RecipeMatch(
                    percentage = 67,
                    ingredients = listOf(
                        RecipeIngredientMatch(RecipeIngredient("Patata", "4 u"), owned = true),
                        RecipeIngredientMatch(RecipeIngredient("Huevo", "6 u"), owned = true),
                        RecipeIngredientMatch(RecipeIngredient("Cebolla", "1 u"), owned = false)
                    )
                ),
                isSaved = true,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeDetailContentNotSavedPreview() {
    RecetairoTheme {
        RecipeDetailContent(
            state = RecipeDetailUiState(
                recipe = Recipe(
                    id = 0,
                    title = "Ensalada César con Pollo a la Plancha",
                    difficulty = RecipeDifficulty.MEDIA,
                    durationMinutes = 20,
                    servings = 2,
                    ingredients = listOf(RecipeIngredient(name = "Lechuga", quantityText = "1 u")),
                    steps = listOf("Lavar la lechuga.", "Cortar el pollo.")
                ),
                match = RecipeMatch(
                    percentage = 0,
                    ingredients = listOf(
                        RecipeIngredientMatch(RecipeIngredient("Lechuga", "1 u"), owned = false)
                    )
                ),
                isSaved = false,
                isLoading = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeDetailContentNotFoundPreview() {
    RecetairoTheme {
        RecipeDetailContent(
            state = RecipeDetailUiState(isLoading = false, notFound = true),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeDetailContentLoadingPreview() {
    RecetairoTheme {
        RecipeDetailContent(
            state = RecipeDetailUiState(isLoading = true),
            onNavigateBack = {}
        )
    }
}
