package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchFoodHistoryUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    operator fun invoke(query: String): Flow<List<String>> = foodRepository.searchFoodNames(query)
}
