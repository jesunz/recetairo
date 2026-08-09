package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteFoodsUseCaseTest {

    private fun buildUseCase(result: Result<Unit>): DeleteFoodsUseCase {
        val fakeRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun searchFoodNames(query: String): Flow<List<String>> =
                throw UnsupportedOperationException()
            override fun getAllFoods(): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
            override suspend fun deleteFood(foodId: Long): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> = result
            override fun getFoodById(foodId: Long): Flow<Result<Food?>> =
                throw UnsupportedOperationException()
        }
        return DeleteFoodsUseCase(fakeRepository)
    }

    @Test
    fun should_returnSuccess_when_repositoryDeleteSucceeds() = runTest {
        // Given
        val useCase = buildUseCase(Result.Success(Unit))

        // When
        val result = useCase(listOf(1L, 2L))

        // Then
        assertTrue(result is Result.Success)
    }

    @Test
    fun should_returnError_when_repositoryDeleteFails() = runTest {
        // Given
        val exception = RuntimeException("db failure")
        val useCase = buildUseCase(Result.Error(exception))

        // When
        val result = useCase(listOf(1L, 2L))

        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
