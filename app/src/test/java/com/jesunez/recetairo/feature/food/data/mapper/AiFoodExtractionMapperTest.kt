package com.jesunez.recetairo.feature.food.data.mapper

import com.jesunez.recetairo.feature.food.data.dto.AiFoodItemDto
import com.jesunez.recetairo.feature.food.domain.model.FoodUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiFoodExtractionMapperTest {

    @Test
    fun should_mapEmoji_whenPresentAndValid() {
        val dto = AiFoodItemDto(name = "Queso", quantity = "1", category = "otros", emoji = "🧀")

        val result = dto.toDomain()

        assertEquals("🧀", result.emoji)
    }

    @Test
    fun should_mapNullEmoji_whenInvalid() {
        val dto = AiFoodItemDto(name = "Queso", quantity = "1", category = "otros", emoji = "cheese")

        val result = dto.toDomain()

        assertNull(result.emoji)
    }

    @Test
    fun should_mapUnitLabelToFoodUnit_when_unitIsRecognized() {
        val dto = AiFoodItemDto(name = "Aceite", quantity = "1", unit = "litros", category = "otros")

        val result = dto.toDomain()

        assertEquals(FoodUnit.LITROS, result.unit)
    }

    @Test
    fun should_defaultToUnidades_when_unitIsNullOrUnrecognized() {
        val dto = AiFoodItemDto(name = "Queso", quantity = "1", unit = null, category = "otros")

        val result = dto.toDomain()

        assertEquals(FoodUnit.UNIDADES, result.unit)
    }
}
