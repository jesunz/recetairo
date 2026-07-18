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

class GetCategorySummariesUseCaseTest {

    /**
     * R1/R13: filling every category to 0 when it has no foods is repository behavior —
     * this fake mirrors it so the tests exercise the disabled-category case through the
     * use case boundary.
     */
    private fun buildUseCase(foods: List<Food>): GetCategorySummariesUseCase {
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
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> {
                val counts = foods.groupingBy { FoodCategory.fromLabel(it.category) }.eachCount()
                val summaries = FoodCategory.entries.map { category ->
                    CategorySummary(category, counts[category] ?: 0)
                }
                return flowOf(Result.Success(summaries))
            }
        }
        return GetCategorySummariesUseCase(fakeRepository)
    }

    @Test
    fun should_returnAllNineCategories_when_repositorySucceeds() = runTest {
        // Given
        val useCase = buildUseCase(emptyList())

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(FoodCategory.entries.size, data.size)
        assertEquals(FoodCategory.entries.toSet(), data.map { it.category }.toSet())
    }

    @Test
    fun should_returnZeroItemCount_when_categoryHasNoStoredFoods() = runTest {
        // Given: R13 — an empty category must appear disabled with 0 items, never omitted
        val foods = listOf(Food(name = "Leche", quantity = 1.0, category = FoodCategory.LACTEOS.label()))
        val useCase = buildUseCase(foods)

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        val emptyCategory = data.first { it.category == FoodCategory.FRUTAS }
        assertEquals(0, emptyCategory.itemCount)
    }

    @Test
    fun should_returnRealItemCount_when_categoryHasStoredFoods() = runTest {
        // Given
        val foods = listOf(
            Food(name = "Leche", quantity = 1.0, category = FoodCategory.LACTEOS.label()),
            Food(name = "Yogur", quantity = 1.0, category = FoodCategory.LACTEOS.label())
        )
        val useCase = buildUseCase(foods)

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Success)
        val lacteos = (result as Result.Success).data.first { it.category == FoodCategory.LACTEOS }
        assertEquals(2, lacteos.itemCount)
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
                throw UnsupportedOperationException()
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                flowOf(Result.Error(exception))
        }
        val useCase = GetCategorySummariesUseCase(fakeRepository)

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
