// Feature: insertar-alimentos-despensa, Property 5
package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.CategorySummary
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsertMultipleFoodsUseCasePropertyTest {

    private fun buildUseCase(failureDecisions: List<Boolean>): InsertMultipleFoodsUseCase {
        var callIndex = 0
        val fakeRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> {
                val shouldFail = failureDecisions.getOrElse(callIndex++) { false }
                return if (shouldFail) Result.Error(RuntimeException("Simulated failure for ${food.name}"))
                else Result.Success(Unit)
            }
            override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun searchFoodNames(query: String): Flow<List<String>> = emptyFlow()
            override fun getAllFoods(): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getFoodsByCategory(category: FoodCategory): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getExpiringSoonFoods(limit: Int?): Flow<Result<List<Food>>> =
                throw UnsupportedOperationException()
            override fun getCategorySummaries(): Flow<Result<List<CategorySummary>>> =
                throw UnsupportedOperationException()
        }
        return InsertMultipleFoodsUseCase(InsertFoodUseCase(fakeRepository))
    }

    @Test
    fun should_accountForEveryFood_when_someInsertionsFail() {
        // R23, R24: successCount + failures.size must always equal the input list size
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.bind(Arb.string(1..50), Arb.boolean()) { n, f -> n to f }, 0..20)
            ) { pairs ->
                val foods = pairs.map { (name, _) -> Food(name = name, quantity = 1.0) }
                val failFlags = pairs.map { it.second }
                val summary = buildUseCase(failFlags)(foods)

                assertEquals(
                    "R23/R24: successCount + failures.size must equal foods.size",
                    foods.size,
                    summary.successCount + summary.failures.size
                )
            }
        }
    }

    @Test
    fun should_matchExpectedCounts_when_repositoryFailsRandomly() {
        // R27: each food is processed independently; a failure in one must not block others
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.bind(Arb.string(1..50), Arb.boolean()) { n, f -> n to f }, 1..20)
            ) { pairs ->
                val foods = pairs.map { (name, _) -> Food(name = name, quantity = 1.0) }
                val failFlags = pairs.map { it.second }
                val expectedSuccesses = failFlags.count { !it }
                val expectedFailures = failFlags.count { it }

                val summary = buildUseCase(failFlags)(foods)

                assertEquals("R27: success count must match non-failing insertions", expectedSuccesses, summary.successCount)
                assertEquals("R27: failure count must match failing insertions", expectedFailures, summary.failures.size)
            }
        }
    }

    @Test
    fun should_includeFailedFoodNamesInSummary_when_insertionFails() {
        // R24, R28: each failed food is identified by name in the InsertionSummary
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.bind(Arb.string(1..50), Arb.boolean()) { n, f -> n to f }, 0..20)
            ) { pairs ->
                val foods = pairs.map { (name, _) -> Food(name = name, quantity = 1.0) }
                val failFlags = pairs.map { it.second }
                val expectedFailedNames = pairs.filter { it.second }.map { it.first }

                val summary = buildUseCase(failFlags)(foods)

                val actualFailedNames = summary.failures.map { it.food.name }
                assertEquals(
                    "R24/R28: failures must contain exactly the names of foods that failed",
                    expectedFailedNames,
                    actualFailedNames
                )
            }
        }
    }

    @Test
    fun should_returnEmptyFailures_when_allInsertionsSucceed() {
        // R23: when no insertion fails, failures list is empty and successCount equals list size
        runBlocking {
            checkAll(100, Arb.list(Arb.string(1..50), 0..20)) { names ->
                val foods = names.map { name -> Food(name = name, quantity = 1.0) }
                val summary = buildUseCase(List(foods.size) { false })(foods)

                assertEquals("R23: successCount must equal total when all succeed", foods.size, summary.successCount)
                assertTrue("R23: failures must be empty when all succeed", summary.failures.isEmpty())
            }
        }
    }
}
