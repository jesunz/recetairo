// Feature: afinar-extraccion-ticket, T11
package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.AiFoodExtractionRepository
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessReceiptOcrUseCaseTest {

    @Test
    fun should_keepPurelyNumericLine_when_aiFoodExtractionRepositorySucceeds() = runTest {
        // R10: NumericNoiseFilter only runs inside degradedModeResult. With the
        // Extractor_IA available, a purely numeric OCR line must not be discarded by
        // the filter -- the use case delegates entirely to whatever the AI returns.
        val ocrItems = listOf(
            OcrFoodItem(
                name = "7",
                quantity = "1",
                expiryDate = "31/12/2026",
                confidence = 0.9f,
                isVerified = false
            )
        )
        val aiItems = listOf(
            OcrFoodItem(
                name = "7",
                quantity = "1",
                expiryDate = "31/12/2026",
                confidence = 0.9f,
                isVerified = false
            )
        )
        val ocrRepository = object : OcrRepository {
            override suspend fun extractItemsFromImage(imageBytes: ByteArray): Result<List<OcrFoodItem>> =
                Result.Success(ocrItems)
        }
        val aiFoodExtractionRepository = object : AiFoodExtractionRepository {
            override suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>> =
                Result.Success(aiItems)
        }
        val useCase = ProcessReceiptOcrUseCase(ocrRepository, aiFoodExtractionRepository)

        // When
        val result = useCase(ByteArray(0))

        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(
            "R10: the numeric line must survive untouched, exactly as the Extractor_IA returned it",
            aiItems,
            data.items
        )
        assertFalse("R10: a successful Extractor_IA path must not be Modo_Degradado", data.isDegradedMode)
    }
}
