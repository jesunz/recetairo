package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.FailedItem
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.InsertionSummary
import timber.log.Timber
import javax.inject.Inject

class InsertMultipleFoodsUseCase @Inject constructor(
    private val insertFoodUseCase: InsertFoodUseCase
) {
    suspend operator fun invoke(foods: List<Food>): InsertionSummary {
        var successCount = 0
        val failures = mutableListOf<FailedItem>()

        foods.forEach { food ->
            when (val result = insertFoodUseCase(food)) {
                is Result.Success -> successCount++
                is Result.Error -> {
                    val reason = result.message ?: result.exception.message ?: "Unknown error"
                    Timber.e(result.exception, "Failed to insert food: ${food.name}")
                    failures.add(FailedItem(food = food, reason = reason))
                }
                is Result.Loading -> Unit
            }
        }

        return InsertionSummary(successCount = successCount, failures = failures)
    }
}
