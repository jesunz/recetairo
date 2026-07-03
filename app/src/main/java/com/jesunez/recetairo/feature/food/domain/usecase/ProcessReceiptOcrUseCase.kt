package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class ProcessReceiptOcrUseCase @Inject constructor(
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): Result<List<OcrFoodItem>> = try {
        withTimeout(30_000.milliseconds) {
            ocrRepository.extractItemsFromImage(imageBytes)
        }
    } catch (e: TimeoutCancellationException) {
        Result.Error(e, "OCR timeout: processing exceeded 30 seconds")
    } catch (e: Exception) {
        Result.Error(e)
    }
}
