package com.jesunez.recetairo.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class NumericQuantityFieldTest {

    @Test
    fun should_keepOnlyDigits_when_inputHasNoSeparator() {
        assertEquals("123", filterQuantityInput("123"))
        assertEquals("", filterQuantityInput(""))
        assertEquals("12", filterQuantityInput("a1b2c"))
    }

    @Test
    fun should_keepSingleSeparator_when_inputHasOneDecimalSeparator() {
        assertEquals("1,5", filterQuantityInput("1,5"))
        assertEquals("1.5", filterQuantityInput("1.5"))
        assertEquals(",5", filterQuantityInput(",5"))
    }

    @Test
    fun should_dropExtraSeparatorsAndOtherCharacters_when_inputIsMalformed() {
        assertEquals("1.56", filterQuantityInput("1.5.6"))
        assertEquals("1,56", filterQuantityInput("1,5,6"))
        assertEquals("1.56", filterQuantityInput("1.5,6"))
        assertEquals("125", filterQuantityInput("1 2-5kg"))
    }
}
