package com.jesunez.recetairo.feature.food.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesunez.recetairo.core.ui.component.CategoryDropdown
import com.jesunez.recetairo.core.ui.component.DateField
import com.jesunez.recetairo.core.ui.component.NumericQuantityField
import com.jesunez.recetairo.core.ui.component.UnitDropdown
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.ui.component.emoji

internal fun isQuantityValid(quantity: String): Boolean =
    quantity.replace(',', '.').toDoubleOrNull() != null

@Composable
fun OcrItemListView(
    items: List<OcrFoodItem>,
    isLoading: Boolean,
    onItemEdited: (Int, OcrFoodItem) -> Unit,
    onItemRemoved: (Int) -> Unit,
    onItemCategoryChanged: (Int, FoodCategory) -> Unit,
    onConfirmSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Productos detectados en el ticket",
                style = MaterialTheme.typography.titleMedium
            )
        }
        itemsIndexed(items) { index, item ->
            OcrItemRow(
                item = item,
                onItemChanged = { updated -> onItemEdited(index, updated) },
                onRemove = { onItemRemoved(index) },
                onCategoryChanged = { category -> onItemCategoryChanged(index, category) }
            )
        }
        item {
            Button(
                onClick = onConfirmSelection,
                enabled = !isLoading &&
                    items.any { it.isSelected } &&
                    items.none { it.isSelected && !isQuantityValid(it.quantity) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .semantics { contentDescription = "Confirmar selección de productos" }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Confirmar selección")
                }
            }
        }
    }
}

@Composable
private fun OcrItemRow(
    item: OcrFoodItem,
    onItemChanged: (OcrFoodItem) -> Unit,
    onRemove: () -> Unit,
    onCategoryChanged: (FoodCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    // R4, R5: needsReview items get a warning border/background + "Revisar" label so the
    // user notices them, but stay fully editable/selectable like any other item.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (item.needsReview) contentDescription = "Producto a revisar: ${item.name}"
            },
        colors = if (item.needsReview) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
        border = if (item.needsReview) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else {
            null
        }
    ) {
        Column(Modifier.padding(12.dp)) {
            if (item.needsReview) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Revisar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { checked -> onItemChanged(item.copy(isSelected = checked)) },
                    modifier = Modifier.semantics { contentDescription = "Seleccionar ${item.name}" }
                )
                Text(
                    text = item.emoji ?: item.category.emoji(),
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .semantics { contentDescription = "Emoji del producto" }
                )
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onItemChanged(item.copy(name = it)) },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Nombre del producto" }
                )
            }
            val quantityInvalid = item.isSelected && !isQuantityValid(item.quantity)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                NumericQuantityField(
                    value = item.quantity,
                    onValueChange = { onItemChanged(item.copy(quantity = it)) },
                    // Narrower than the date field: DateField's trailing calendar icon eats into
                    // its usable width, so give it more of the shared row to avoid clipping "Caducidad"
                    modifier = Modifier.weight(0.8f),
                    isError = quantityInvalid,
                    fieldContentDescription = "Cantidad del producto"
                )
                Spacer(Modifier.width(8.dp))
                DateField(
                    value = item.expiryDate,
                    onValueChange = { onItemChanged(item.copy(expiryDate = it)) },
                    label = "Caducidad",
                    modifier = Modifier.weight(1.2f),
                    fieldContentDescription = "Fecha de caducidad del producto"
                )
            }
            // Shown below the row (not as a per-field supportingText) so both fields keep
            // the same height regardless of validation state
            if (quantityInvalid) {
                Text(
                    text = "Cantidad obligatoria",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
            CategoryDropdown(
                selected = item.category,
                onCategoryChanged = onCategoryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            UnitDropdown(
                selected = item.unit,
                onUnitChanged = { unit -> onItemChanged(item.copy(unit = unit)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            TextButton(
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = "Eliminar ${item.name} de la lista" }
            ) {
                Text("Eliminar")
            }
        }
    }
}
