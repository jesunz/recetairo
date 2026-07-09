// Feature: mejora-extraccion-ia-ticket, Property 12
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.AiFoodExtractionRepository
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import com.jesunez.recetairo.feature.food.domain.usecase.InsertFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.InsertMultipleFoodsUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.ProcessReceiptOcrUseCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptScanViewModelPropertyTest {

    private val testDispatcher = StandardTestDispatcher()

    private val validLabels = FoodCategory.entries.map { it.label() }.toSet()
    private val insertedFoods = mutableListOf<Food>()

    private fun buildViewModel(ocrItems: List<OcrFoodItem>): ReceiptScanViewModel {
        insertedFoods.clear()
        val ocrRepository = object : OcrRepository {
            override suspend fun extractItemsFromImage(imageBytes: ByteArray): Result<List<OcrFoodItem>> =
                Result.Success(ocrItems)
        }
        val aiFoodExtractionRepository = object : AiFoodExtractionRepository {
            override suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>> =
                Result.Success(ocrItems)
        }
        val foodRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> {
                insertedFoods.add(food)
                return Result.Success(Unit)
            }
            override suspend fun insertFoods(foods: List<Food>): Result<Unit> = Result.Success(Unit)
            override fun searchFoodNames(query: String): Flow<List<String>> = flowOf(emptyList())
        }
        return ReceiptScanViewModel(
            ProcessReceiptOcrUseCase(ocrRepository, aiFoodExtractionRepository),
            InsertMultipleFoodsUseCase(InsertFoodUseCase(foodRepository))
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun should_insertEveryFoodWithNonEmptyNameAndValidCategory_when_confirmingCategorizedTicketItems() =
        runTest(testDispatcher) {
            // P12, R10, R14: every food inserted from the ticket flow keeps a non-empty name
            // and a category belonging to the 9 valid FoodCategory labels.
            checkAll(
                100,
                Arb.list(
                    Arb.bind(
                        Arb.string(1..30).filter { it.isNotBlank() },
                        Arb.enum<FoodCategory>()
                    ) { name, category -> name to category },
                    1..10
                )
            ) { pairs ->
                val ocrItems = pairs.map { (name, category) ->
                    OcrFoodItem(
                        name = name,
                        quantity = "1",
                        expiryDate = "31/12/2026",
                        category = category,
                        confidence = 0.9f,
                        isVerified = true
                    )
                }
                val viewModel = buildViewModel(ocrItems)

                viewModel.onImageCaptured(ByteArray(0))
                advanceUntilIdle()
                viewModel.onConfirmSelection()
                advanceUntilIdle()

                assertTrue("P12/R10: every selected item must be inserted", insertedFoods.size == ocrItems.size)
                insertedFoods.forEach { food ->
                    assertFalse("P12/R14: inserted food name must not be blank", food.name.isBlank())
                    assertTrue(
                        "P12/R14: inserted food category must belong to the 9 valid FoodCategory labels",
                        food.category in validLabels
                    )
                }
            }
        }
}
