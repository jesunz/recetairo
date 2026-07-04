package com.jesunez.recetairo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesunez.recetairo.ui.theme.RecetairoTheme

/**
 * ExpiringSoonSection implements R6: "display a 'Por Vencer' (Expiring Soon) 
 * horizontal scroll section with food item placeholders."
 */
@Composable
fun ExpiringSoonSection(
    modifier: Modifier = Modifier,
    onViewAllClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Por Vencer",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "Ver todo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(expiringSoonPlaceholders) { item ->
                ExpiringFoodCard(item = item)
            }
        }
    }
}

@Composable
private fun ExpiringFoodCard(
    item: ExpiringFoodPlaceholder,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(256.dp),
        shape = CircleShape, // Pill shape
        colors = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular container for emoji
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 28.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    maxLines = 1
                )
                
                Text(
                    text = if (item.daysLeft == 1) "Vence mañana" else "Vence en ${item.daysLeft} días",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private data class ExpiringFoodPlaceholder(
    val name: String,
    val emoji: String,
    val daysLeft: Int,
    val color: Color
)

private val expiringSoonPlaceholders = listOf(
    ExpiringFoodPlaceholder("Leche Entera", "🥛", 1, Color(0xFFBA1A1A)), // Error/Tertiary tint
    ExpiringFoodPlaceholder("Aguacate", "🥑", 3, Color(0xFF645E49)), // Secondary tint
    ExpiringFoodPlaceholder("Pechuga Pollo", "🍗", 2, Color(0xFFBA1A1A)),
    ExpiringFoodPlaceholder("Yogur Griego", "🍦", 5, Color(0xFF645E49))
)

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
fun ExpiringSoonSectionPreview() {
    RecetairoTheme {
        ExpiringSoonSection(modifier = Modifier.padding(vertical = 16.dp))
    }
}
