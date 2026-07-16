package com.jesunez.recetairo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.ui.theme.RecetairoTheme

/**
 * CategoryGrid implements R1: displays the 9 [FoodCategory] values with their real item count.
 * A category with `itemCount == 0` is rendered disabled (R13).
 */
@Composable
fun CategoryGrid(
    categories: List<CategorySummary>,
    modifier: Modifier = Modifier,
    onCategoryClick: (FoodCategory) -> Unit = {}
) {
    val totalItems = categories.sumOf { it.itemCount }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categorías de Despensa",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "$totalItems artículos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Using Column/Row instead of LazyVerticalGrid to facilitate nesting in a scrollable HomeScreen
        categories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { summary ->
                    CategoryCard(
                        summary = summary,
                        onClick = { onCategoryClick(summary.category) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    summary: CategorySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = summary.itemCount > 0
    val label = summary.category.label()

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, ${summary.itemCount} artículos"
            },
        shape = MaterialTheme.shapes.large, // 32dp
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular container for category emoji
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 1f else 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = summary.category.emoji(),
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${summary.itemCount} ítems",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.4f)
            )
        }
    }
}

private fun FoodCategory.emoji(): String = when (this) {
    FoodCategory.LACTEOS -> "🧀"
    FoodCategory.CARNE -> "🥩"
    FoodCategory.PESCADO -> "🐟"
    FoodCategory.MARISCO -> "🦐"
    FoodCategory.FRUTAS -> "🍎"
    FoodCategory.VERDURAS -> "🥦"
    FoodCategory.PAN -> "🍞"
    FoodCategory.CEREALES -> "🌾"
    FoodCategory.OTROS -> "📦"
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
fun CategoryGridPreview() {
    RecetairoTheme {
        CategoryGrid(
            modifier = Modifier.padding(vertical = 16.dp),
            categories = FoodCategory.entries.mapIndexed { index, category ->
                CategorySummary(category = category, itemCount = if (index % 3 == 0) 0 else index * 2)
            }
        )
    }
}
