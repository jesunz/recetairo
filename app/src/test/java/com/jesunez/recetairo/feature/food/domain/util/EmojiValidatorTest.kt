package com.jesunez.recetairo.feature.food.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmojiValidatorTest {

    @Test
    fun should_returnEmoji_when_looksLikeValidEmoji() {
        assertEquals("🧀", "🧀".asValidFoodEmoji())
        assertEquals("🥛", " 🥛 ".asValidFoodEmoji())
        // Skin-tone modifier / ZWJ sequences stay within the length allowance.
        assertEquals("🧑‍🌾", "🧑‍🌾".asValidFoodEmoji())
    }

    @Test
    fun should_returnNull_when_blankOrContainsAsciiLetters() {
        assertNull((null as String?).asValidFoodEmoji())
        assertNull("".asValidFoodEmoji())
        assertNull("   ".asValidFoodEmoji())
        assertNull("cheese".asValidFoodEmoji())
        assertNull(":cheese:".asValidFoodEmoji())
        assertNull("🧀2".asValidFoodEmoji())
    }

    @Test
    fun should_returnNull_when_tooLongToBeAnEmoji() {
        assertNull("🧀🥛🥚🍞🍎".asValidFoodEmoji())
    }
}
