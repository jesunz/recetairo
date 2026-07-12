// Feature: afinar-extraccion-ticket, bugfix found by Property 14 (T10) while implementing T12
package com.jesunez.recetairo.feature.food.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumericNoiseFilterTest {

    @Test
    fun should_treatAsNoise_when_lineIsPurelyNumeric() {
        assertTrue(NumericNoiseFilter.isNoise("974"))
        assertTrue(NumericNoiseFilter.isNoise("12,50"))
    }

    @Test
    fun should_treatAsNoise_when_lineIsQuantityTimesPrice() {
        // "x"/"X" allowed as a multiplication separator only alongside at least one digit
        assertTrue(NumericNoiseFilter.isNoise("2x3"))
        assertTrue(NumericNoiseFilter.isNoise("2X3,50"))
    }

    @Test
    fun should_treatAsNoise_when_lineIsNonFoodToken() {
        assertTrue(NumericNoiseFilter.isNoise("IVA"))
        assertTrue(NumericNoiseFilter.isNoise("total"))
    }

    @Test
    fun should_notTreatAsNoise_when_foodNameIsMadeOnlyOfXLetters() {
        // Regression: "Xxxx" has no digit at all, so it must not match the
        // digit-and-separator char class meant for OCR quantity/price lines.
        assertFalse(NumericNoiseFilter.isNoise("Xxxx"))
        assertFalse(NumericNoiseFilter.isNoise("xxxx"))
    }

    @Test
    fun should_notTreatAsNoise_when_lineIsAValidFoodName() {
        assertFalse(NumericNoiseFilter.isNoise("Manzana"))
        assertFalse(NumericNoiseFilter.isNoise("Pan"))
    }
}
