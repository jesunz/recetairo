package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFoodsByCategoryUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    operator fun invoke(category: FoodCategory): Flow<Result<List<Food>>> =
        foodRepository.getFoodsByCategory(category)
}
