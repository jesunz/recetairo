// Feature: mejora-extraccion-ia-ticket, Property 9
package com.jesunez.recetairo.feature.food.data.repository

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.jesunez.recetairo.core.domain.model.Result
import com.squareup.moshi.Moshi
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking

class FirebaseAiFoodExtractionRepositoryImplPropertyTest {

    private val moshi = Moshi.Builder().build()

    private fun buildRepository(responseJson: String = "[]"): Pair<FirebaseAiFoodExtractionRepositoryImpl, GenerativeModel> {
        val mockResponse = mock<GenerateContentResponse> {
            on { text } doReturn responseJson
        }
        val mockModel = mock<GenerativeModel> {
            onBlocking { generateContent(any<String>()) } doReturn mockResponse
        }
        return FirebaseAiFoodExtractionRepositoryImpl(mockModel, moshi) to mockModel
    }

    @Test
    fun should_sendRawTextUnmodified_when_extractingFoodItems() {
        // P9, R1-R3: the raw OCR text must reach the AI model whole, without trimming or filtering
        runBlocking {
            checkAll(
                100,
                Arb.list(Arb.string(0, 15), 1..8),
                Arb.of("", " ", "\n", "\t", "  \n  ", "\n\n", "\t \n")
            ) { lines, padding ->
                val rawText = "$padding${lines.joinToString("\n")}$padding"
                val (repository, mockModel) = buildRepository()

                repository.extractFoodItems(rawText)

                val promptCaptor = argumentCaptor<String>()
                verifyBlocking(mockModel) { generateContent(promptCaptor.capture()) }

                assertTrue(
                    "P9: the prompt sent to the AI model must contain the raw OCR text verbatim, " +
                        "with no trimming or filtering applied",
                    promptCaptor.firstValue.contains(rawText)
                )
            }
        }
    }

    @Test
    fun should_preserveNameAndSetNeedsReviewTrue_when_modelMarksItemAsNeedingReview() {
        // Feature: afinar-extraccion-ticket, Property 13, R1-R2: a simulated model response
        // with needsReview=true and a partial/noisy name must reach OcrFoodItem with the name
        // untouched (no text added) and needsReview=true.
        val nameAdapter = moshi.adapter(String::class.java)
        runBlocking {
            checkAll(100, Arb.string(1, 20)) { partialName ->
                val nameJson = nameAdapter.toJson(partialName)
                val responseJson = """[{"name":$nameJson,"quantity":"1","category":"otros","needsReview":true}]"""
                val (repository, _) = buildRepository(responseJson)

                val result = repository.extractFoodItems("irrelevant raw text")

                assertTrue("Result must be Success but was: $result", result is Result.Success)
                val items = (result as Result.Success).data

                assertEquals(
                    "P13, R2: name must be preserved exactly as returned by the model, with no text added",
                    partialName,
                    items.single().name
                )
                assertTrue(
                    "P13, R1: needsReview must be true when the model marks the item as such",
                    items.single().needsReview
                )
            }
        }
    }

    @Test
    fun should_normalizeNeedsReviewToFalse_when_fieldIsAbsentFromModelResponse() {
        // Feature: afinar-extraccion-ticket, Property 13, R1: needsReview omitted from the
        // model's JSON response must normalize to false on the resulting OcrFoodItem.
        val nameAdapter = moshi.adapter(String::class.java)
        runBlocking {
            checkAll(100, Arb.string(1, 20)) { name ->
                val nameJson = nameAdapter.toJson(name)
                val responseJson = """[{"name":$nameJson,"quantity":"1","category":"otros"}]"""
                val (repository, _) = buildRepository(responseJson)

                val result = repository.extractFoodItems("irrelevant raw text")

                assertTrue("Result must be Success but was: $result", result is Result.Success)
                val items = (result as Result.Success).data

                assertFalse(
                    "P13, R1: needsReview must normalize to false when absent from the JSON",
                    items.single().needsReview
                )
            }
        }
    }
}
