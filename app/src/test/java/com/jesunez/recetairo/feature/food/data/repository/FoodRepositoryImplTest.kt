// Feature: gestion-despensa, T2
package com.jesunez.recetairo.feature.food.data.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.data.dao.CategoryCountRow
import com.jesunez.recetairo.feature.food.data.dao.FoodDao
import com.jesunez.recetairo.feature.food.data.entity.FoodEntity
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class FoodRepositoryImplTest {

    @Test
    fun should_returnSuccess_when_deleteSucceeds() = runBlocking {
        // R7: deleteFood/deleteFoods must return Success when the DAO deletion completes normally
        val foodDao = mock<FoodDao>()
        val repository = FoodRepositoryImpl(foodDao)

        val deleteFoodResult = repository.deleteFood(1L)
        val deleteFoodsResult = repository.deleteFoods(listOf(1L, 2L))

        assertTrue(deleteFoodResult is Result.Success)
        assertTrue(deleteFoodsResult is Result.Success)
        verify(foodDao).deleteById(1L)
        verify(foodDao).deleteByIds(listOf(1L, 2L))
    }

    @Test
    fun should_returnError_when_deleteThrows() = runBlocking {
        // R10: a DAO failure must surface as Result.Error, not propagate as an exception
        val exception = RuntimeException("db failure")
        val foodDao = mock<FoodDao> {
            onBlocking { deleteById(1L) } doThrow exception
            onBlocking { deleteByIds(listOf(1L, 2L)) } doThrow exception
        }
        val repository = FoodRepositoryImpl(foodDao)

        val deleteFoodResult = repository.deleteFood(1L)
        val deleteFoodsResult = repository.deleteFoods(listOf(1L, 2L))

        assertTrue(deleteFoodResult is Result.Error)
        assertEquals(exception, (deleteFoodResult as Result.Error).exception)
        assertTrue(deleteFoodsResult is Result.Error)
        assertEquals(exception, (deleteFoodsResult as Result.Error).exception)
    }

    @Test
    fun should_emitNull_when_foodNotFound() = runBlocking {
        // R15: a missing/deleted food must surface as Success(null), not an error
        val foodDao = mock<FoodDao> {
            on { getById(1L) } doReturn flowOf(null)
        }
        val repository = FoodRepositoryImpl(foodDao)

        val result = repository.getFoodById(1L).first()

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }

    @Test
    fun should_emitFood_when_foodExists() = runBlocking {
        val entity = FoodEntity(
            id = 1L,
            name = "Leche",
            quantity = 1.0,
            unit = "L",
            category = "Lácteos",
            expiryDate = null,
            imageUrl = null,
            insertedAt = 0L
        )
        val foodDao = mock<FoodDao> {
            on { getById(1L) } doReturn flowOf(entity)
        }
        val repository = FoodRepositoryImpl(foodDao)

        val result = repository.getFoodById(1L).first()

        assertTrue(result is Result.Success)
        assertEquals("Leche", (result as Result.Success).data?.name)
    }

    @Test
    fun should_sumCounts_when_multipleRawCategoriesFallBackToSameCategory() = runBlocking {
        // Rows with different literal `category` text (stale/unrecognized values) can both
        // fall back to the same FoodCategory via fromLabel(); their counts must add up
        // instead of the last one silently overwriting the others.
        val foodDao = mock<FoodDao> {
            on { getCategoryCounts() } doReturn flowOf(
                listOf(
                    CategoryCountRow(category = "Otros", itemCount = 3),
                    CategoryCountRow(category = "", itemCount = 2),
                    CategoryCountRow(category = "raw-off-taxonomy", itemCount = 1)
                )
            )
        }
        val repository = FoodRepositoryImpl(foodDao)

        val result = repository.getCategorySummaries().first()

        assertTrue(result is Result.Success)
        val otros = (result as Result.Success).data.first { it.category == FoodCategory.OTROS }
        assertEquals(6, otros.itemCount)
    }
}
