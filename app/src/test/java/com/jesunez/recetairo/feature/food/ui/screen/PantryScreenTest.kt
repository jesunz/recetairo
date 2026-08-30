package com.jesunez.recetairo.feature.food.ui.screen

import com.jesunez.recetairo.feature.food.domain.model.Food
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PantryScreenTest {

    private fun food(expiryDate: LocalDate? = null) = Food(
        id = 1,
        name = "Leche",
        quantity = 1.0,
        unit = "L",
        category = "Lácteos",
        expiryDate = expiryDate
    )

    @Test
    fun should_reportNoExpiry_when_foodHasNoExpiryDate() {
        val presentation = pantryFoodRowPresentation(food(), isSelectionMode = false, isSelected = false)

        assertNull(presentation.expiryText)
        assertFalse(presentation.hasExpiry)
        assertFalse(presentation.isExpired)
    }

    @Test
    fun should_reportExpired_when_expiryDateIsInThePast() {
        val presentation = pantryFoodRowPresentation(
            food(LocalDate.now().minusDays(1)),
            isSelectionMode = false,
            isSelected = false
        )

        assertEquals("Caducado", presentation.expiryText)
        assertTrue(presentation.hasExpiry)
        assertTrue(presentation.isExpired)
    }

    @Test
    fun should_reportVenceHoy_when_expiryDateIsToday() {
        val presentation = pantryFoodRowPresentation(food(LocalDate.now()), isSelectionMode = false, isSelected = false)

        assertEquals("Vence hoy", presentation.expiryText)
        assertFalse(presentation.isExpired)
    }

    @Test
    fun should_appendSelectionState_when_inSelectionMode() {
        val selected = pantryFoodRowPresentation(food(), isSelectionMode = true, isSelected = true)
        val unselected = pantryFoodRowPresentation(food(), isSelectionMode = true, isSelected = false)
        val outsideSelectionMode = pantryFoodRowPresentation(food(), isSelectionMode = false, isSelected = false)

        assertTrue(selected.description.endsWith(", seleccionado"))
        assertTrue(unselected.description.endsWith(", no seleccionado"))
        assertFalse(outsideSelectionMode.description.contains("seleccionado"))
    }
}
