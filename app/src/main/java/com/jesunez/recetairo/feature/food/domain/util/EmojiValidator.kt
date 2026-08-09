package com.jesunez.recetairo.feature.food.domain.util

// A real emoji is short even with skin-tone modifiers or ZWJ sequences; anything longer is
// almost certainly a word or shortcode the AI returned instead of a symbol.
private const val MAX_EMOJI_UTF16_LENGTH = 8

fun String?.asValidFoodEmoji(): String? {
    val trimmed = this?.trim()
    if (trimmed.isNullOrEmpty()) return null
    if (trimmed.length > MAX_EMOJI_UTF16_LENGTH) return null
    if (trimmed.any { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }) return null
    return trimmed
}
