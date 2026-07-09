// Feature: mejora-extraccion-ia-ticket, Property 11
package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.AiFoodExtractionRepository
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessReceiptOcrUseCasePropertyTest {

    private enum class AiFailureMode { NETWORK_ERROR, SERVICE_ERROR, TIMEOUT }

    private fun buildUseCase(ocrItems: List<OcrFoodItem>, failureMode: AiFailureMode): ProcessReceiptOcrUseCase {
        val ocrRepository = object : OcrRepository {
            override suspend fun extractItemsFromImage(imageBytes: ByteArray): Result<List<OcrFoodItem>> =
                Result.Success(ocrItems)
        }
        val aiFoodExtractionRepository = object : AiFoodExtractionRepository {
            override suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>> =
                when (failureMode) {
                    AiFailureMode.NETWORK_ERROR ->
                        Result.Error(IOException("sin conectividad"), "sin conectividad")
                    AiFailureMode.SERVICE_ERROR ->
                        Result.Error(Exception("error del servicio"), "error del servicio")
                    AiFailureMode.TIMEOUT -> {
                        delay(40_000.milliseconds)
                        Result.Success(ocrItems)
                    }
                }
        }
        return ProcessReceiptOcrUseCase(ocrRepository, aiFoodExtractionRepository)
    }

    @Test
    fun should_activateDegradedModeWithConfidenceFilter_when_aiFoodExtractionRepositoryFails() =
        runTest(StandardTestDispatcher()) {
            // P11, R7, R9: network exception, service error, or timeout on the Extractor_IA
            // all fall back to Modo_Degradado: confidence >= 70% filter, category OTROS
            checkAll(
                100,
                Arb.list(Arb.numericFloat(0f, 1f), 1..10),
                Arb.of(AiFailureMode.entries)
            ) { confidences, failureMode ->
                val ocrItems = confidences.mapIndexed { index, confidence ->
                    OcrFoodItem(
                        name = "item$index",
                        quantity = "1",
                        expiryDate = "31/12/2026",
                        confidence = confidence,
                        isVerified = false
                    )
                }
                val useCase = buildUseCase(ocrItems, failureMode)

                val result = useCase(ByteArray(0))

                assertTrue(
                    "P11: Modo_Degradado must still yield a Result.Success with the filtered items",
                    result is Result.Success
                )
                val data = (result as Result.Success).data
                val expectedItems = ocrItems
                    .filter { it.confidence >= 0.70f }
                    .map { it.copy(category = FoodCategory.OTROS, isVerified = true) }

                assertEquals(
                    "P11: Modo_Degradado must keep only items with confidence >= 70%, all with category OTROS",
                    expectedItems,
                    data.items
                )
                assertTrue("P11: the result must be flagged as Modo_Degradado", data.isDegradedMode)
            }
        }
}
