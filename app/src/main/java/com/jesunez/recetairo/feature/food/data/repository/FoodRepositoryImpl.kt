package com.jesunez.recetairo.feature.food.data.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.data.dao.CategoryCountRow
import com.jesunez.recetairo.feature.food.data.dao.FoodDao
import com.jesunez.recetairo.feature.food.data.entity.FoodEntity
import com.jesunez.recetairo.feature.food.data.mapper.toDomain
import com.jesunez.recetairo.feature.food.data.mapper.toEntity
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
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

    override fun getAllFoods(): Flow<Result<List<Food>>> =
        foodDao.getAll()
            .map<List<FoodEntity>, Result<List<Food>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .catch { e -> emit(Result.Error(e)) }

    override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
        foodDao.getByCategory(category.label())
            .map<List<FoodEntity>, Result<List<Food>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .catch { e -> emit(Result.Error(e)) }

    override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> {
        val threshold = LocalDate.now().plusDays(EXPIRING_SOON_THRESHOLD_DAYS).toString()
        return foodDao.getExpiringSoon(threshold)
            .map<List<FoodEntity>, Result<List<Food>>> { entities ->
                val foods = entities.map { it.toDomain() }
                Result.Success(if (limit != null) foods.take(limit) else foods)
            }
            .catch { e -> emit(Result.Error(e)) }
    }

    override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
        foodDao.getCategoryCounts()
            .map<List<CategoryCountRow>, Result<List<CategorySummary>>> { rows ->
                val counts = rows.associate { FoodCategory.fromLabel(it.category) to it.itemCount }
                Result.Success(
                    FoodCategory.entries.map { category -> CategorySummary(category, counts[category] ?: 0) }
                )
            }
            .catch { e -> emit(Result.Error(e)) }

    override suspend fun deleteFood(foodId: Long): Result<Unit> = try {
        foodDao.deleteById(foodId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> = try {
        foodDao.deleteByIds(foodIds)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override fun getFoodById(foodId: Long): Flow<Result<Food?>> =
        foodDao.getById(foodId)
            .map<FoodEntity?, Result<Food?>> { entity ->
                Result.Success(entity?.toDomain())
            }
            .catch { e -> emit(Result.Error(e)) }

    private companion object {
        const val EXPIRING_SOON_THRESHOLD_DAYS = 3L
    }
}
