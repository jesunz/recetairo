package com.jesunez.recetairo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onAddManually: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Despensa", style = MaterialTheme.typography.headlineMedium)

            Button(
                onClick = onAddManually,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .semantics { contentDescription = "Añadir alimento manualmente" }
            ) {
                Text("Añadir alimento manualmente")
            }

            Button(
                onClick = onScanBarcode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .semantics { contentDescription = "Escanear código de barras" }
            ) {
                Text("Escanear código de barras")
            }

            Button(
                onClick = onScanReceipt,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .semantics { contentDescription = "Escanear ticket de compra" }
            ) {
                Text("Escanear ticket de compra")
            }
        }
    }
}
