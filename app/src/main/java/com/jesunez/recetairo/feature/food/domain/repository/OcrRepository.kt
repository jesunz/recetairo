package com.jesunez.recetairo.feature.food.domain.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem

interface OcrRepository {
    suspend fun extractItemsFromImage(imageBytes: ByteArray): Result<List<OcrFoodItem>>
}
