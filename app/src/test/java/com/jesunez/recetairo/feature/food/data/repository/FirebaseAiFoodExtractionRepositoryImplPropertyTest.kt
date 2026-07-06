// Feature: mejora-extraccion-ia-ticket, Property 9
package com.jesunez.recetairo.feature.food.data.repository

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.squareup.moshi.Moshi
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking

class FirebaseAiFoodExtractionRepositoryImplPropertyTest {

    private val moshi = Moshi.Builder().build()

    private fun buildRepository(): Pair<FirebaseAiFoodExtractionRepositoryImpl, GenerativeModel> {
        val mockResponse = mock<GenerateContentResponse> {
            on { text } doReturn "[]"
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
}
