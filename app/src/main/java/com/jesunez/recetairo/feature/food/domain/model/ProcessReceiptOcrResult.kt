package com.jesunez.recetairo.feature.food.domain.model

data class ProcessReceiptOcrResult(
    val items: List<OcrFoodItem>,
    val isDegradedMode: Boolean = false
)
