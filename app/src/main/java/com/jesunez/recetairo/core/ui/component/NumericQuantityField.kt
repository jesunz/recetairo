package com.jesunez.recetairo.core.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType

private val QUANTITY_SEPARATORS = charArrayOf(',', '.')

// Keeps only digits and, at most, the first decimal separator (`,` or `.`) found in the
// candidate value — any other character, or a repeated separator, is silently dropped (R1, R2).
fun filterQuantityInput(input: String): String {
    val filtered = StringBuilder()
    var hasSeparator = false
    for (char in input) {
        when {
            char.isDigit() -> filtered.append(char)
            char in QUANTITY_SEPARATORS && !hasSeparator -> {
                filtered.append(char)
                hasSeparator = true
            }
        }
    }
    return filtered.toString()
}

@Composable
fun NumericQuantityField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Cantidad",
    isError: Boolean = false,
    supportingText: String? = null,
    fieldContentDescription: String = "Campo cantidad"
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(filterQuantityInput(it)) },
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        modifier = modifier.semantics { contentDescription = fieldContentDescription },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
