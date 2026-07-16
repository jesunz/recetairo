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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.ui.theme.RecetairoTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ExpiringSoonSection implements R4: horizontal scroll of foods expiring soon (max 5, sorted by
 * expiry date ascending). R11: visually distinguishes already-expired items from upcoming ones.
 */
@Composable
fun ExpiringSoonSection(
    items: List<Food>,
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
            TextButton(
                onClick = onViewAllClick,
                modifier = Modifier.semantics { contentDescription = "Ver todo lo próximo a vencer" }
            ) {
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
            items(items, key = { it.id }) { food ->
                ExpiringFoodCard(food = food)
            }
        }
    }
}

@Composable
private fun ExpiringFoodCard(
    food: Food,
    modifier: Modifier = Modifier
) {
    // Invariant: R3/GetExpiringSoonFoodsUseCase only return foods with a non-null expiryDate.
    val expiryDate = requireNotNull(food.expiryDate) { "Expiring soon food must have an expiryDate" }
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
    val isExpired = daysLeft < 0
    val expiryText = when {
        isExpired -> "Caducado"
        daysLeft == 0L -> "Vence hoy"
        daysLeft == 1L -> "Vence mañana"
        else -> "Vence en $daysLeft días"
    }
    val accentColor = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    Card(
        modifier = modifier
            .width(256.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${food.name}, $expiryText"
            },
        shape = CircleShape, // Pill shape
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.12f)
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
                    text = FoodCategory.fromLabel(food.category).emoji(),
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    ),
                    maxLines = 1
                )

                Text(
                    text = expiryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
fun ExpiringSoonSectionPreview() {
    RecetairoTheme {
        ExpiringSoonSection(
            modifier = Modifier.padding(vertical = 16.dp),
            items = listOf(
                Food(id = 1, name = "Leche Entera", quantity = 1.0, category = "Lácteos", expiryDate = LocalDate.now()),
                Food(id = 2, name = "Aguacate", quantity = 2.0, category = "Frutas", expiryDate = LocalDate.now().plusDays(3)),
                Food(id = 3, name = "Pechuga Pollo", quantity = 1.0, category = "Carne", expiryDate = LocalDate.now().minusDays(1))
            )
        )
    }
}
