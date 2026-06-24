package com.jesunez.recetairo.feature.food.domain.model

data class InsertionSummary(
    val successCount: Int,
    val failures: List<FailedItem>
)
