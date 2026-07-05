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
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.checkAll
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
import java.io.ByteArrayOutputStream

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
    fun should_returnAllRecognizedLines_when_confidenceVaries() {
        // Feature: mejora-extraccion-ia-ticket, R1: no confidence filtering, all lines pass through
        runBlocking {
            checkAll(100, Arb.list(Arb.numericFloat(0f, 1f), 0..20)) { confidences ->
                val repository = buildRepository(confidences)
                val result = repository.extractItemsFromImage(MINIMAL_PNG)

                assertTrue(
                    "Result must be Success but was: $result",
                    result is Result.Success
                )
                val items = (result as Result.Success).data

                assertEquals(
                    "R1: item count must equal the number of recognized lines regardless of confidence",
                    confidences.size,
                    items.size
                )
            }
        }
    }

    @Test
    fun should_preserveConfidence_when_extractingLines() {
        // Feature: mejora-extraccion-ia-ticket, R1: original line confidence must be preserved
        runBlocking {
            checkAll(100, Arb.list(Arb.numericFloat(0f, 1f), 1..20)) { confidences ->
                val repository = buildRepository(confidences)
                val result = repository.extractItemsFromImage(MINIMAL_PNG)

                assertTrue(
                    "Result must be Success but was: $result",
                    result is Result.Success
                )
                val items = (result as Result.Success).data

                assertEquals(
                    "R1: each returned item must preserve its original line confidence",
                    confidences.sorted(),
                    items.map { it.confidence }.sorted()
                )
            }
        }
    }

    @Test
    fun should_setIsVerifiedFalse_when_itemIsExtracted() {
        // Feature: mejora-extraccion-ia-ticket, R1: classification is deferred to the AI extractor
        runBlocking {
            checkAll(100, Arb.list(Arb.numericFloat(0f, 1f), 1..10)) { confidences ->
                val repository = buildRepository(confidences)
                val result = repository.extractItemsFromImage(MINIMAL_PNG)

                assertTrue(
                    "Result must be Success but was: $result",
                    result is Result.Success
                )
                val items = (result as Result.Success).data
                assertTrue(
                    "R1: all items returned by OcrRepositoryImpl must have isVerified == false",
                    items.none { it.isVerified }
                )
            }
        }
    }
}
