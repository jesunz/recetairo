package com.jesunez.recetairo.feature.recipe.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.ui.component.emoji
import com.jesunez.recetairo.ui.theme.RecetairoTheme

/**
 * Accordion section for one FoodCategory in Selector_Ingredientes: header toggles expand/collapse
 * (R6), body shows the category's Foods as selectable chips (R7). Specific to this screen, not
 * moved to core/ui/component (docs/conventions.md: only components used in 2+ features).
 */
@Composable
fun IngredientCategorySection(
    category: FoodCategory,
    foods: List<Food>,
    isExpanded: Boolean,
    selectedNames: Set<String>,
    onCategoryToggled: () -> Unit,
    onIngredientToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCategoryToggled)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${category.label()}, ${foods.size} alimentos, " +
                        if (isExpanded) "expandido" else "colapsado"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.emoji(), fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${category.label()} (${foods.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isExpanded) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                foods.forEach { food ->
                    val isSelected = food.name in selectedNames
                    FilterChip(
                        selected = isSelected,
                        onClick = { onIngredientToggled(food.name) },
                        label = { Text(food.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "${food.name}, " +
                                if (isSelected) "seleccionado" else "no seleccionado"
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun IngredientCategorySectionExpandedPreview() {
    RecetairoTheme {
        IngredientCategorySection(
            category = FoodCategory.VERDURAS,
            foods = listOf(
                Food(id = 1, name = "Tomate", quantity = 3.0, unit = "u", category = "Verduras"),
                Food(id = 2, name = "Cebolla", quantity = 2.0, unit = "u", category = "Verduras"),
                Food(id = 3, name = "Pimiento Rojo", quantity = 1.0, unit = "u", category = "Verduras")
            ),
            isExpanded = true,
            selectedNames = setOf("Tomate"),
            onCategoryToggled = {},
            onIngredientToggled = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun IngredientCategorySectionCollapsedPreview() {
    RecetairoTheme {
        IngredientCategorySection(
            category = FoodCategory.LACTEOS,
            foods = listOf(
                Food(id = 1, name = "Leche Entera", quantity = 1.0, unit = "L", category = "Lácteos")
            ),
            isExpanded = false,
            selectedNames = emptySet(),
            onCategoryToggled = {},
            onIngredientToggled = {}
        )
    }
}
