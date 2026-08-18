// Feature: mejora-extraccion-ia-ticket, Property 10
package com.jesunez.recetairo.feature.food.data.mapper

import com.jesunez.recetairo.feature.food.data.dto.AiFoodItemDto
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.FoodUnit
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AiFoodExtractionMapperPropertyTest {

    private val validLabels = FoodCategory.entries.map { it.label() }
    private val validUnitLabels = FoodUnit.entries.map { it.label() }

    @Test
    fun should_mapToUnidades_when_unitDoesNotMatchAnyValidLabel() {
        // Mirrors P10 for FoodUnit: any unit string not matching one of the valid labels
        // maps to UNIDADES, the same fallback OcrFoodItem already defaults to.
        runBlocking {
            checkAll(
                100,
                Arb.string().filter { candidate -> validUnitLabels.none { it.equals(candidate, ignoreCase = true) } }
            ) { unit ->
                val dto = AiFoodItemDto(name = "item", quantity = null, unit = unit, category = "otros")

                val result = dto.toDomain()

                assertEquals(
                    "An unrecognized unit label must fall back to FoodUnit.UNIDADES",
                    FoodUnit.UNIDADES,
                    result.unit
                )
            }
        }
    }

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

    @Test
    fun should_preserveNameAndNeedsReview_when_mappingDtoToDomain() {
        // Feature: afinar-extraccion-ticket, Property 13, R1-R2: the mapper must not alter
        // "name" (no characters added or removed) nor override "needsReview" as set by the
        // AI extractor.
        runBlocking {
            checkAll(100, Arb.string(0, 30), Arb.boolean()) { name, needsReview ->
                val dto = AiFoodItemDto(name = name, quantity = null, category = "otros", needsReview = needsReview)

                val result = dto.toDomain()

                assertEquals(
                    "P13, R2: name must be preserved verbatim, without adding or removing characters",
                    name,
                    result.name
                )
                assertEquals(
                    "P13, R1: needsReview must pass through unchanged from the DTO",
                    needsReview,
                    result.needsReview
                )
            }
        }
    }
}
