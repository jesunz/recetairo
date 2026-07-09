package com.jesunez.recetairo.feature.food.data.repository

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.OcrRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            .filter { it.text.isNotBlank() }
            .map { line ->
                OcrFoodItem(
                    name = line.text.trim(),
                    quantity = "",
                    expiryDate = "",
                    confidence = line.confidence,
                    isVerified = false
                )
            }

        Result.Success(items)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
