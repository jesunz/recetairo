// Feature: insertar-alimentos-despensa, Property 4
package com.jesunez.recetairo.feature.food.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import com.jesunez.recetairo.core.domain.model.Result
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.checkAll
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OcrRepositoryImplPropertyTest {

    companion object {
        // Valid 1x1 transparent PNG generated via Android Bitmap — works with Robolectric
        private val MINIMAL_PNG: ByteArray by lazy {
            val bos = ByteArrayOutputStream()
            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            bos.toByteArray()
        }
    }

    @Before
    fun setUp() {
        // ML Kit requires context initialization; ContentProviders don't auto-run in Robolectric.
        // The catch handles subsequent tests where it was already initialized in the same JVM session.
        try {
            MlKit.initialize(ApplicationProvider.getApplicationContext<Context>())
        } catch (_: IllegalStateException) { /* Already initialized */ }
    }

    private fun buildRepository(confidences: List<Float>): OcrRepositoryImpl {
        val mockLines = confidences.map { confidence ->
            mock<Text.Line> {
                on { this.confidence } doReturn confidence
                on { this.text } doReturn "Food Item"
            }
        }

        val mockBlock = mock<Text.TextBlock> {
            on { this.lines } doReturn mockLines
        }

        val mockVisionText = mock<Text> {
            on { this.textBlocks } doReturn listOf(mockBlock)
        }

        // Task calls addOnSuccessListener synchronously to avoid Looper threading issues in tests
        val mockTask = mock<Task<Text>> {
            on { addOnSuccessListener(any<OnSuccessListener<Text>>()) } doAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                inv.getArgument<OnSuccessListener<Text>>(0).onSuccess(mockVisionText)
                inv.mock as Task<Text>
            }
            on { addOnFailureListener(any<OnFailureListener>()) } doAnswer { inv ->
                inv.mock as Task<Text>
            }
        }

        val fakeRecognizer = mock<TextRecognizer> {
            on { process(any<InputImage>()) } doReturn mockTask
        }

        return OcrRepositoryImpl(fakeRecognizer)
    }

    @Test
    fun should_includeOnlyItemsAboveThreshold_when_confidenceVaries() {
        // R22: items with confidence >= OCR_CONFIDENCE_THRESHOLD pass the filter
        runBlocking {
            checkAll(100, Arb.list(Arb.numericFloat(0f, 1f), 0..20)) { confidences ->
                val repository = buildRepository(confidences)
                val result = repository.extractItemsFromImage(MINIMAL_PNG)

                assertTrue(
                    "Result must be Success but was: $result",
                    result is Result.Success
                )
                val items = (result as Result.Success).data
                val expectedCount = confidences.count { it >= OcrRepositoryImpl.OCR_CONFIDENCE_THRESHOLD }

                assertEquals(
                    "R22: item count must equal number of confidences >= ${OcrRepositoryImpl.OCR_CONFIDENCE_THRESHOLD}",
                    expectedCount,
                    items.size
                )
            }
        }
    }

    @Test
    fun should_returnNoItems_when_allConfidencesBelowThreshold() {
        // R22: items below threshold must be filtered out entirely
        runBlocking {
            checkAll(
                100,
                Arb.list(
                    Arb.numericFloat(0f, 1f).filter { it < OcrRepositoryImpl.OCR_CONFIDENCE_THRESHOLD },
                    1..20
                )
            ) { confidences ->
                val repository = buildRepository(confidences)
                val result = repository.extractItemsFromImage(MINIMAL_PNG)

                assertTrue(
                    "Result must be Success but was: $result",
                    result is Result.Success
                )
                assertTrue(
                    "R22: result must be empty when all confidences are below threshold",
                    (result as Result.Success).data.isEmpty()
                )
            }
        }
    }

    @Test
    fun should_setIsVerifiedTrue_when_itemPassesThreshold() {
        // R20: OcrFoodItem.isVerified must be true for all returned items
        runBlocking {
            checkAll(
                100,
                Arb.list(
                    Arb.numericFloat(0f, 1f).filter { it >= OcrRepositoryImpl.OCR_CONFIDENCE_THRESHOLD },
                    1..10
                )
            ) { confidences ->
                val repository = buildRepository(confidences)
                val result = repository.extractItemsFromImage(MINIMAL_PNG)

                assertTrue(
                    "Result must be Success but was: $result",
                    result is Result.Success
                )
                val items = (result as Result.Success).data
                assertTrue(
                    "R20: all items passing the threshold must have isVerified == true",
                    items.all { it.isVerified }
                )
            }
        }
    }
}
