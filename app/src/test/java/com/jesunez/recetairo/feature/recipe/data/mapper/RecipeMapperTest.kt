package com.jesunez.recetairo.feature.recipe.data.mapper

import com.jesunez.recetairo.feature.recipe.data.dto.RecipeIngredientDto
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeMapperTest {

    @Test
    fun should_stripTrailingPlus_when_aiAppendsItToIngredientName() {
        val dto = RecipeIngredientDto(name = "Patatas+", quantity = "500g")

        val result = dto.toDomain()

        assertEquals("Patatas", result.name)
    }

    @Test
    fun should_stripTrailingPlusWithSpace_when_aiAddsSpaceBeforeIt() {
        val dto = RecipeIngredientDto(name = "Patatas +", quantity = "500g")

        val result = dto.toDomain()

        assertEquals("Patatas", result.name)
    }

    @Test
    fun should_keepNameUnchanged_when_noTrailingPlusIsPresent() {
        val dto = RecipeIngredientDto(name = "Patatas", quantity = "500g")

        val result = dto.toDomain()

        assertEquals("Patatas", result.name)
    }
}
