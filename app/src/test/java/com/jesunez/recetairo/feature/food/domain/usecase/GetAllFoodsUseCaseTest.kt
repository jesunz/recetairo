// Feature: consultar-despensa, T13
package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAllFoodsUseCaseTest {

    private fun buildUseCase(result: Result<List<Food>>): GetAllFoodsUseCase {
        val fakeRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun searchFoodNames(query: String): Flow<List<String>> =
                throw UnsupportedOperationException()
            override fun getAllFoods(): Flow<Result<List<Food>>> = flowOf(result)
            override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
            override suspend fun deleteFood(foodId: Long): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun getFoodById(foodId: Long): Flow<Result<Food?>> =
                throw UnsupportedOperationException()
        }
        return GetAllFoodsUseCase(fakeRepository)
    }

    @Test
    fun should_returnAllFoodsSortedByRepository_when_repositorySucceeds() = runTest {
        // Given
        val foods = listOf(
            Food(name = "Leche", quantity = 1.0),
            Food(name = "Pan", quantity = 2.0)
        )
        val useCase = buildUseCase(Result.Success(foods))

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(foods, (result as Result.Success).data)
    }

    @Test
    fun should_returnError_when_repositoryEmitsError() = runTest {
        // Given
        val exception = RuntimeException("db failure")
        val useCase = buildUseCase(Result.Error(exception))

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
