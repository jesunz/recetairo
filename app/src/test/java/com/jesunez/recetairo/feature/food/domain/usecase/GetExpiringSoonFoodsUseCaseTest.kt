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
import java.time.LocalDate

class GetExpiringSoonFoodsUseCaseTest {

    /**
     * R3: threshold (today + 7 days) is applied by the repository, before the use case
     * ever sees the data — this fake mirrors that behavior so the tests exercise it
     * through the use case boundary.
     */
    private fun buildUseCase(allFoods: List<Food>): GetExpiringSoonFoodsUseCase {
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
            override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> {
                val threshold = LocalDate.now().plusDays(SEVEN_DAY_THRESHOLD)
                val expiringSoon = allFoods
                    .filter { it.expiryDate != null && !it.expiryDate.isAfter(threshold) }
                    .sortedBy { it.expiryDate }
                return flowOf(Result.Success(if (limit != null) expiringSoon.take(limit) else expiringSoon))
            }
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
            override suspend fun deleteFood(foodId: Long): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun getFoodById(foodId: Long): Flow<Result<Food?>> =
                throw UnsupportedOperationException()
        }
        return GetExpiringSoonFoodsUseCase(fakeRepository)
    }

    @Test
    fun should_excludeFoodsBeyondSevenDayThreshold_when_invokedWithoutLimit() = runTest {
        // Given
        val withinThreshold = Food(name = "Leche", quantity = 1.0, expiryDate = LocalDate.now().plusDays(7))
        val beyondThreshold = Food(name = "Conserva", quantity = 1.0, expiryDate = LocalDate.now().plusDays(8))
        val useCase = buildUseCase(listOf(withinThreshold, beyondThreshold))

        // When
        val result = useCase(limit = null).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(listOf("Leche"), (result as Result.Success).data.map { it.name })
    }

    @Test
    fun should_excludeFoodsWithoutExpiryDate_when_invoked() = runTest {
        // Given
        val noExpiry = Food(name = "Sal", quantity = 1.0, expiryDate = null)
        val useCase = buildUseCase(listOf(noExpiry))

        // When
        val result = useCase(limit = null).first()

        // Then
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun should_limitResultsToFive_when_invokedFromHomeWithLimit() = runTest {
        // Given: R4 — Home shows at most 5 items
        val foods = (1..8).map { day ->
            Food(name = "Item$day", quantity = 1.0, expiryDate = LocalDate.now().plusDays(day.toLong()))
        }
        val useCase = buildUseCase(foods)

        // When
        val result = useCase(limit = 5).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(5, (result as Result.Success).data.size)
    }

    @Test
    fun should_returnAllExpiringFoodsWithoutLimit_when_invokedForVerTodo() = runTest {
        // Given: R5 — "View all" removes the 5-item limit
        val foods = (1..7).map { day ->
            Food(name = "Item$day", quantity = 1.0, expiryDate = LocalDate.now().plusDays(day.toLong()))
        }
        val useCase = buildUseCase(foods)

        // When
        val result = useCase(limit = null).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(7, (result as Result.Success).data.size)
    }

    @Test
    fun should_returnError_when_repositoryEmitsError() = runTest {
        // Given
        val exception = RuntimeException("db failure")
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
                flowOf(Result.Error(exception))
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
            override suspend fun deleteFood(foodId: Long): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun deleteFoods(foodIds: List<Long>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun getFoodById(foodId: Long): Flow<Result<Food?>> =
                throw UnsupportedOperationException()
        }
        val useCase = GetExpiringSoonFoodsUseCase(fakeRepository)

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    private companion object {
        const val SEVEN_DAY_THRESHOLD = 7L
    }
}
