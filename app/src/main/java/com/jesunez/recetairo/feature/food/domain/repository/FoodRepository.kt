package com.jesunez.recetairo.feature.food.domain.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    suspend fun insertFood(food: Food): Result<Unit>
    suspend fun insertFoods(foods: List<Food>): Result<Unit>
    fun searchFoodNames(query: String): Flow<List<String>>
}
