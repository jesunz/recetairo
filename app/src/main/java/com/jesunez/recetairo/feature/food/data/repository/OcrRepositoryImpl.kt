package com.jesunez.recetairo.feature.food.data.repository

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class OcrRepositoryImpl @Inject constructor(
    private val recognizer: TextRecognizer
) : OcrRepository {

    override suspend fun extractItemsFromImage(imageBytes: ByteArray): Result<List<OcrFoodItem>> = try {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return Result.Error(IllegalArgumentException("Cannot decode image bytes"))
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val visionText = suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
        }

        val items = visionText.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val confidence = line.confidence
                if (confidence >= OCR_CONFIDENCE_THRESHOLD && line.text.isNotBlank()) {
                    OcrFoodItem(
                        name = line.text.trim(),
                        quantity = "",
                        expiryDate = "",
                        confidence = confidence,
                        isVerified = true
                    )
                } else null
            }

        Result.Success(items)
    } catch (e: Exception) {
        Result.Error(e)
    }

    companion object {
        const val OCR_CONFIDENCE_THRESHOLD = 0.70f
    }
}
