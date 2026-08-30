package com.jesunez.recetairo.feature.recipe.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jesunez.recetairo.ui.theme.RecetairoTheme

private const val MIN_SERVINGS = 1
private const val MAX_SERVINGS = 4

/**
 * Raciones_Objetivo selector for Selector_Ingredientes: single-choice chips from 1 to 4,
 * no keyboard entry (R1, R2). Specific to this screen, not moved to core/ui/component
 * (docs/conventions.md: only components used in 2+ features).
 */
@Composable
fun ServingsSelector(
    servings: Int,
    onServingsSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Raciones",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in MIN_SERVINGS..MAX_SERVINGS) {
                val isSelected = servings == option
                val description = if (option == 1) "1 ración" else "$option raciones"
                FilterChip(
                    selected = isSelected,
                    onClick = { onServingsSelected(option) },
                    label = { Text("$option") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "$description, " +
                            if (isSelected) "seleccionado" else "no seleccionado"
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun ServingsSelectorPreview() {
    RecetairoTheme {
        ServingsSelector(servings = 1, onServingsSelected = {})
    }
}
