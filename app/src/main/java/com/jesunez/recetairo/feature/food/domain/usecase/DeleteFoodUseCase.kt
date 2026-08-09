package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import javax.inject.Inject

class DeleteFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    suspend operator fun invoke(foodId: Long): Result<Unit> = foodRepository.deleteFood(foodId)
}
