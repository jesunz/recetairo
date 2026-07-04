package com.jesunez.recetairo.feature.food.domain.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem

interface AiFoodExtractionRepository {
    suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>>
}
