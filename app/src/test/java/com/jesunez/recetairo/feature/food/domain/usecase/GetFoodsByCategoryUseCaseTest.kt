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

class GetFoodsByCategoryUseCaseTest {

    private fun buildUseCase(allFoods: List<Food>, requestedCategories: MutableList<FoodCategory>): GetFoodsByCategoryUseCase {
        val fakeRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun searchFoodNames(query: String): Flow<List<String>> =
                throw UnsupportedOperationException()
            override fun getAllFoods(): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> {
                requestedCategories.add(category)
                return flowOf(Result.Success(allFoods.filter { it.category == category.label() }))
            }
            override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
        }
        return GetFoodsByCategoryUseCase(fakeRepository)
    }

    @Test
    fun should_requestExactCategoryFromRepository_when_invoked() = runTest {
        // Given
        val requestedCategories = mutableListOf<FoodCategory>()
        val useCase = buildUseCase(emptyList(), requestedCategories)

        // When
        useCase(FoodCategory.LACTEOS).first()

        // Then
        assertEquals(listOf(FoodCategory.LACTEOS), requestedCategories)
    }

    @Test
    fun should_returnOnlyFoodsOfRequestedCategory_when_repositorySucceeds() = runTest {
        // Given
        val foods = listOf(
            Food(name = "Leche", quantity = 1.0, category = FoodCategory.LACTEOS.label()),
            Food(name = "Manzana", quantity = 1.0, category = FoodCategory.FRUTAS.label())
        )
        val useCase = buildUseCase(foods, mutableListOf())

        // When
        val result = useCase(FoodCategory.LACTEOS).first()

        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(listOf("Leche"), data.map { it.name })
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
                flowOf(Result.Error(exception))
            override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
        }
        val useCase = GetFoodsByCategoryUseCase(fakeRepository)

        // When
        val result = useCase(FoodCategory.CARNE).first()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
