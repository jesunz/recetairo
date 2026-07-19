package com.jesunez.recetairo.feature.food.domain.model

import java.time.Instant
import java.time.LocalDate

data class Food(
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String = "",
    val category: String = "",
    val expiryDate: LocalDate? = null,
    val imageUrl: String? = null,
    val insertedAt: Instant = Instant.now(),
    val emoji: String? = null
)
