// Feature: insertar-alimentos-despensa, Property 1
package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFoodHistoryUseCasePropertyTest {

    private fun buildUseCase(storedNames: List<String>): SearchFoodHistoryUseCase {
        val fakeRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> =
                throw UnsupportedOperationException()
            override suspend fun insertFoods(foods: List<Food>): Result<Unit> =
                throw UnsupportedOperationException()
            override fun searchFoodNames(query: String): Flow<List<String>> =
                flowOf(storedNames.filter { it.contains(query) }.sorted().take(5))
        }
        return SearchFoodHistoryUseCase(fakeRepository)
    }

    @Test
    fun should_returnOnlyMatchingNames_when_queryMatchesSomeNames() {
        // R3: every returned name must contain the query as a substring
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.string(1..30), 0..20),
                Arb.string(1..5)
            ) { names, query ->
                val results = buildUseCase(names)(query).first()
                assertTrue(
                    "R3: every result must contain the query '$query'",
                    results.all { it.contains(query) }
                )
            }
        }
    }

    @Test
    fun should_returnResultsInAlphabeticalOrder_when_multipleNamesMatch() {
        // R3: results must be sorted alphabetically
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.string(1..30), 0..20),
                Arb.string(0..5)
            ) { names, query ->
                val results = buildUseCase(names)(query).first()
                assertEquals(
                    "R3: results must be sorted alphabetically",
                    results.sorted(),
                    results
                )
            }
        }
    }

    @Test
    fun should_returnAtMostFiveResults_when_manyNamesMatch() {
        // R3: at most 5 suggestions visible
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.string(1..30), 0..20),
                Arb.string(0..5)
            ) { names, query ->
                val results = buildUseCase(names)(query).first()
                assertTrue(
                    "R3: result count must not exceed 5 (got ${results.size})",
                    results.size <= 5
                )
            }
        }
    }

    @Test
    fun should_returnEmptyList_when_noNameContainsQuery() {
        // R5: no suggestions shown when no name matches
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.string(1..30), 0..20),
                Arb.string(1..5)
            ) { names, query ->
                val results = buildUseCase(names)(query).first()
                if (names.none { it.contains(query) }) {
                    assertTrue(
                        "R5: result must be empty when no name contains '$query'",
                        results.isEmpty()
                    )
                }
            }
        }
    }
}
