package com.jesunez.recetairo.feature.food.data.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.data.dao.FoodDao
import com.jesunez.recetairo.feature.food.data.mapper.toEntity
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FoodRepositoryImpl @Inject constructor(
    private val foodDao: FoodDao
) : FoodRepository {

    override suspend fun insertFood(food: Food): Result<Unit> = try {
        foodDao.insert(food.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun insertFoods(foods: List<Food>): Result<Unit> {
        foods.forEach { food ->
            val result = insertFood(food)
            if (result is Result.Error) return result
        }
        return Result.Success(Unit)
    }

    override fun searchFoodNames(query: String): Flow<List<String>> =
        foodDao.searchNames(query)
}
