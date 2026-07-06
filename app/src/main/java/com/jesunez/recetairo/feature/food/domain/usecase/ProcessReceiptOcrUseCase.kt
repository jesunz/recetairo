package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.model.ProcessReceiptOcrResult
import com.jesunez.recetairo.feature.food.domain.repository.AiFoodExtractionRepository
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class ProcessReceiptOcrUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val aiFoodExtractionRepository: AiFoodExtractionRepository
) {
    suspend operator fun invoke(
        imageBytes: ByteArray,
        onOcrCompleted: () -> Unit = {}
    ): Result<ProcessReceiptOcrResult> {
        var ocrItems: List<OcrFoodItem>? = null
        return try {
            withTimeout(30_000.milliseconds) {
                when (val ocrResult = ocrRepository.extractItemsFromImage(imageBytes)) {
                    is Result.Success -> {
                        ocrItems = ocrResult.data
                        onOcrCompleted()
                        classify(ocrResult.data)
                    }
                    is Result.Error -> Result.Error(ocrResult.exception, ocrResult.message)
                    is Result.Loading -> Result.Success(ProcessReceiptOcrResult(items = emptyList()))
                }
            }
        } catch (e: TimeoutCancellationException) {
            ocrItems?.let { Result.Success(degradedModeResult(it)) }
                ?: Result.Error(e, "OCR timeout: processing exceeded 30 seconds")
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun classify(ocrItems: List<OcrFoodItem>): Result<ProcessReceiptOcrResult> {
        val rawText = ocrItems.joinToString("\n") { it.name }
        return when (val aiResult = aiFoodExtractionRepository.extractFoodItems(rawText)) {
            is Result.Success -> Result.Success(ProcessReceiptOcrResult(items = aiResult.data))
            is Result.Error -> Result.Success(degradedModeResult(ocrItems))
            is Result.Loading -> Result.Success(degradedModeResult(ocrItems))
        }
    }

    private fun degradedModeResult(ocrItems: List<OcrFoodItem>): ProcessReceiptOcrResult =
        ProcessReceiptOcrResult(
            items = ocrItems
                .filter { it.confidence >= CONFIDENCE_THRESHOLD }
                .map { it.copy(category = FoodCategory.OTROS, isVerified = true) },
            isDegradedMode = true
        )

    private companion object {
        const val CONFIDENCE_THRESHOLD = 0.70f
    }
}
