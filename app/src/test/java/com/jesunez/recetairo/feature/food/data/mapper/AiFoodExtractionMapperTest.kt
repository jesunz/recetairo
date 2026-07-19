package com.jesunez.recetairo.feature.food.data.mapper

import com.jesunez.recetairo.feature.food.data.dto.AiFoodItemDto
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
}
