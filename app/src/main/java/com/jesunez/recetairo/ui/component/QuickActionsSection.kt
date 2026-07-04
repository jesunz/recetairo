package com.jesunez.recetairo.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesunez.recetairo.ui.theme.RecetairoTheme

/**
 * QuickActionsSection implements R7-R11: "display an 'Acciones Rápidas' 
 * (Quick Actions) section with three functional buttons."
 * 
 * Updated to match the "Digital Organic" visual reference with chunky
 * pill-shaped buttons of equal height.
 */
@Composable
fun QuickActionsSection(
    onScanReceipt: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = 112.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )

        // Prominent Button: Escanear Ticket
        QuickActionButton(
            onClick = onScanReceipt,
            title = "Escanear Ticket",
            subtitle = "Sube tu compra automáticamente",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight)
        )

        // Secondary Buttons: Código de Barras & Entrada Manual
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionButtonVertical(
                onClick = onScanBarcode,
                title = "Código de Barras",
                icon = Icons.Default.QrCodeScanner,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(1f)
            )

            QuickActionButtonVertical(
                onClick = onAddManually,
                title = "Entrada Manual",
                icon = Icons.Default.EditNote,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    onClick: () -> Unit,
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun QuickActionButtonVertical(
    onClick: () -> Unit,
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
fun QuickActionsSectionPreview() {
    RecetairoTheme {
        QuickActionsSection(
            onScanReceipt = {},
            onScanBarcode = {},
            onAddManually = {},
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}
