package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import javax.inject.Inject

class InsertFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    suspend operator fun invoke(food: Food): Result<Unit> = foodRepository.insertFood(food)
}
