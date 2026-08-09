package com.jesunez.recetairo.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jesunez.recetairo.feature.food.domain.model.FoodUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdown(
    selected: FoodUnit,
    onUnitChanged: (FoodUnit) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Unidad de medida") },
            isError = isError,
            supportingText = if (isError && supportingText != null) {
                { Text(supportingText) }
            } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .semantics { contentDescription = "Unidad de medida del producto: ${selected.label()}" }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FoodUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label()) },
                    onClick = {
                        onUnitChanged(unit)
                        expanded = false
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Seleccionar unidad ${unit.label()}"
                    }
                )
            }
        }
    }
}
