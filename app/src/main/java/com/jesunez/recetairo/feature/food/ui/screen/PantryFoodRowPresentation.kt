package com.jesunez.recetairo.feature.food.ui.screen

import com.jesunez.recetairo.feature.food.domain.model.Food
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// R11: distinguish already-expired foods from those expiring soon; foods without an
// expiryDate (e.g. non-perishables) show no expiry indicator and use a neutral tint.
internal data class PantryFoodRowPresentation(
    val quantityText: String,
    val expiryText: String?,
    val hasExpiry: Boolean,
    val isExpired: Boolean,
    val description: String
)

internal fun pantryFoodRowPresentation(
    food: Food,
    isSelectionMode: Boolean,
    isSelected: Boolean
): PantryFoodRowPresentation {
    val daysLeft = food.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val expiryText = when {
        daysLeft == null -> null
        daysLeft < 0 -> "Caducado"
        daysLeft == 0L -> "Vence hoy"
        daysLeft == 1L -> "Vence mañana"
        else -> "Vence en $daysLeft días"
    }
    val quantityText = "${food.quantity} ${food.unit}".trim()
    val selectionText = when {
        !isSelectionMode -> ""
        isSelected -> ", seleccionado"
        else -> ", no seleccionado"
    }
    return PantryFoodRowPresentation(
        quantityText = quantityText,
        expiryText = expiryText,
        hasExpiry = daysLeft != null,
        isExpired = daysLeft != null && daysLeft < 0,
        description = listOfNotNull(food.name, quantityText, expiryText).joinToString(", ") + selectionText
    )
}
