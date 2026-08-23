package com.jesunez.recetairo.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import com.jesunez.recetairo.ui.theme.RecetairoTheme

/**
 * Reused by RecipeListScreen (Menu_Recetas, no checkbox) and GeneratedRecipesScreen
 * (Recetas_Generadas, checkbox visible) — moved to core/ui/component per docs/conventions.md
 * (2+ features).
 */
@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val selectionText = when {
        onCheckedChange == null -> ""
        checked -> ", seleccionada"
        else -> ", no seleccionada"
    }
    val description = "${recipe.title}, ${recipe.durationMinutes} minutos, " +
        "dificultad ${recipe.difficulty.label()}$selectionText"

    Card(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.large, // 32dp, matching CategoryGrid's card shape
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🍽️", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onCheckedChange != null) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.semantics {
                            contentDescription = "Seleccionar ${recipe.title}"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${recipe.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = recipe.difficulty.label(),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeCardPreview() {
    RecetairoTheme {
        RecipeCard(
            recipe = Recipe(
                id = 1,
                title = "Tortilla de Patatas",
                difficulty = RecipeDifficulty.FACIL,
                durationMinutes = 30,
                servings = 4,
                ingredients = listOf(RecipeIngredient(name = "Patata", quantityText = "4 u")),
                steps = listOf("Pelar y cortar las patatas.")
            ),
            onClick = {},
            modifier = Modifier.width(180.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun RecipeCardWithCheckboxPreview() {
    RecetairoTheme {
        RecipeCard(
            recipe = Recipe(
                id = 1,
                title = "Ensalada César con Pollo a la Plancha",
                difficulty = RecipeDifficulty.MEDIA,
                durationMinutes = 20,
                servings = 2,
                ingredients = listOf(RecipeIngredient(name = "Lechuga", quantityText = "1 u")),
                steps = listOf("Lavar la lechuga.")
            ),
            onClick = {},
            checked = true,
            onCheckedChange = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
