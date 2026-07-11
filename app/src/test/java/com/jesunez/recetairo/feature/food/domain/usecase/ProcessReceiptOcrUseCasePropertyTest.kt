// Feature: mejora-extraccion-ia-ticket, Property 11
package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.AiFoodExtractionRepository
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
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

    // Same closed list as NumericNoiseFilter.NON_FOOD_TOKENS (Tokens_No_Alimentarios, R7).
    // Duplicated here on purpose: the test must assert against the documented closed list,
    // not by reaching into the filter's private implementation.
    private val nonFoodTokens = setOf(
        "TEL", "TELEFONO", "CIF", "NIF", "IVA", "TOTAL", "SUBTOTAL",
        "CAMBIO", "TARJETA", "EFECTIVO", "TICKET", "FACTURA"
    )

    private val validFoodNameArb: Arb<String> = Arb.list(Arb.of(('a'..'z').toList()), 4..10)
        .map { chars -> chars.joinToString("").replaceFirstChar(Char::uppercase) }
        .filter { name -> name.uppercase() !in nonFoodTokens }

    private val numericNoiseLineArb: Arb<String> = Arb.list(
        Arb.of(listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ',', '-', '/', 'x', 'X', ' ')),
        1..8
    ).map { it.joinToString("") }

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

    @Test
    fun should_excludeNumericAndTokenLines_and_keepValidFoodLines_when_inDegradedMode() =
        runTest(StandardTestDispatcher()) {
            // Feature: afinar-extraccion-ticket, Property 14, R6-R8: on a mix of valid food
            // names, purely numeric lines, and Tokens_No_Alimentarios, all with confidence
            // >= 70%, Modo_Degradado must exclude the numeric/token lines and keep only the
            // valid food names.
            checkAll(
                100,
                Arb.list(validFoodNameArb, 1..5),
                Arb.list(numericNoiseLineArb, 0..5),
                Arb.list(Arb.of(nonFoodTokens.toList()), 0..5)
            ) { validNames, numericLines, tokenLines ->
                fun toItem(name: String) = OcrFoodItem(
                    name = name,
                    quantity = "1",
                    expiryDate = "31/12/2026",
                    confidence = 0.9f,
                    isVerified = false
                )
                val ocrItems = (validNames + numericLines + tokenLines).map(::toItem).shuffled()
                val useCase = buildUseCase(ocrItems, AiFailureMode.SERVICE_ERROR)

                val result = useCase(ByteArray(0))

                assertTrue(
                    "P14: Modo_Degradado must still yield a Result.Success",
                    result is Result.Success
                )
                val data = (result as Result.Success).data

                assertEquals(
                    "P14, R6-R8: only the valid food names must survive the combined filters, " +
                        "in a confidence-independent order (all inputs are already >= 70%)",
                    validNames.sorted(),
                    data.items.map { it.name }.sorted()
                )
                assertTrue("P14: the result must be flagged as Modo_Degradado", data.isDegradedMode)
            }
        }
}
