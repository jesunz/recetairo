// Feature: mejora-extraccion-ia-ticket, Property 10
package com.jesunez.recetairo.feature.food.data.mapper

import com.jesunez.recetairo.feature.food.data.dto.AiFoodItemDto
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AiFoodExtractionMapperPropertyTest {

    private val validLabels = FoodCategory.entries.map { it.label() }

    @Test
    fun should_mapToOtros_when_categoryDoesNotMatchAnyValidLabel() {
        // P10, R4-R5: any category string not matching one of the 9 valid labels maps to OTROS
        runBlocking {
            checkAll(
                100,
                Arb.string().filter { candidate -> validLabels.none { it.equals(candidate, ignoreCase = true) } }
            ) { category ->
                val dto = AiFoodItemDto(name = "item", quantity = null, category = category)

                val result = dto.toDomain()

                assertEquals(
                    "P10: an unrecognized category label must fall back to FoodCategory.OTROS",
                    FoodCategory.OTROS,
                    result.category
                )
            }
        }
    }
}
