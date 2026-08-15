package com.jesunez.recetairo.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DATE_FIELD_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Keeps the same "dd/MM/yyyy" text contract used by ViewModels/parsers; the picker just
// writes a formatted string back through onValueChange, same as free text entry would.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Fecha de caducidad",
    isError: Boolean = false,
    supportingText: String? = null,
    fieldContentDescription: String = "Campo fecha"
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = { showPicker = true },
                modifier = Modifier.semantics { contentDescription = "Abrir calendario" }
            ) {
                Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null)
            }
        },
        modifier = modifier.semantics { contentDescription = fieldContentDescription }
    )

    if (showPicker) {
        val initialMillis = runCatching { LocalDate.parse(value, DATE_FIELD_FORMATTER) }
            .getOrNull()
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onValueChange(date.format(DATE_FIELD_FORMATTER))
                        }
                        showPicker = false
                    },
                    modifier = Modifier.semantics { contentDescription = "Aceptar fecha seleccionada" }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPicker = false },
                    modifier = Modifier.semantics { contentDescription = "Cancelar selección de fecha" }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
