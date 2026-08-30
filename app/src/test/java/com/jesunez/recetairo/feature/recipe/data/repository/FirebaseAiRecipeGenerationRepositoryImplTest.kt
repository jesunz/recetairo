package com.jesunez.recetairo.feature.recipe.data.repository

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking

class FirebaseAiRecipeGenerationRepositoryImplTest {

    private val moshi = Moshi.Builder().build()

    private fun buildRepository(responseJson: String): FirebaseAiRecipeGenerationRepositoryImpl {
        val mockResponse = mock<GenerateContentResponse> {
            on { text } doReturn responseJson
        }
        val mockModel = mock<GenerativeModel> {
            onBlocking { generateContent(any<String>()) } doReturn mockResponse
        }
        return FirebaseAiRecipeGenerationRepositoryImpl(mockModel, buildAuthenticatedAuth(), moshi)
    }

    private fun buildRepositoryWithModel(
        responseJson: String
    ): Pair<FirebaseAiRecipeGenerationRepositoryImpl, GenerativeModel> {
        val mockResponse = mock<GenerateContentResponse> {
            on { text } doReturn responseJson
        }
        val mockModel = mock<GenerativeModel> {
            onBlocking { generateContent(any<String>()) } doReturn mockResponse
        }
        return FirebaseAiRecipeGenerationRepositoryImpl(mockModel, buildAuthenticatedAuth(), moshi) to mockModel
    }

    private fun buildRepositoryThatThrows(exception: Throwable): FirebaseAiRecipeGenerationRepositoryImpl {
        val mockModel = mock<GenerativeModel> {
            onBlocking { generateContent(any<String>()) } doThrow exception
        }
        return FirebaseAiRecipeGenerationRepositoryImpl(mockModel, buildAuthenticatedAuth(), moshi)
    }

    private fun buildAuthenticatedAuth(): FirebaseAuth = mock {
        on { currentUser } doReturn mock<FirebaseUser>()
    }

    @Test
    fun should_returnThreeMappedRecipes_when_modelReturnsValidResponse() {
        // R13: exactly 3 recipes, each with title/difficulty/duration/servings/ingredients/steps
        val responseJson = """
            [
              {"title":"Tortilla de patatas","difficulty":"Fácil","durationMinutes":30,"servings":4,
               "ingredients":[{"name":"Patatas","quantity":"4 unidades"},{"name":"Huevos","quantity":"6 unidades"}],
               "steps":["Pelar y cortar las patatas","Batir los huevos","Freír y cuajar la tortilla"]},
              {"title":"Ensalada César","difficulty":"Media","durationMinutes":20,"servings":2,
               "ingredients":[{"name":"Lechuga","quantity":"1 unidad"}],
               "steps":["Lavar la lechuga","Mezclar con el aliño"]},
              {"title":"Paella","difficulty":"Difícil","durationMinutes":60,"servings":6,
               "ingredients":[{"name":"Arroz","quantity":"500g"}],
               "steps":["Sofreír","Añadir el arroz","Cocinar"]}
            ]
        """.trimIndent()
        val repository = buildRepository(responseJson)

        runBlocking {
            val result = repository.generateRecipes(listOf("Patatas", "Huevos"), 4)

            assertTrue("Result must be Success but was: $result", result is Result.Success)
            val recipes = (result as Result.Success).data

            assertEquals(3, recipes.size)
            assertEquals("Tortilla de patatas", recipes[0].title)
            assertEquals(RecipeDifficulty.FACIL, recipes[0].difficulty)
            assertEquals(RecipeDifficulty.MEDIA, recipes[1].difficulty)
            assertEquals(RecipeDifficulty.DIFICIL, recipes[2].difficulty)
            assertEquals(2, recipes[0].ingredients.size)
            assertEquals(3, recipes[0].steps.size)
        }
    }

    @Test
    fun should_returnError_when_modelReturnsInvalidJson() {
        // R29: a malformed AI response must surface as Result.Error, not crash the app
        val repository = buildRepository("[{")

        runBlocking {
            val result = repository.generateRecipes(listOf("Patatas"), 1)

            assertTrue("Result must be Error but was: $result", result is Result.Error)
        }
    }

    @Test
    fun should_returnError_when_networkExceptionIsThrown() {
        // R29: unlike ticket OCR extraction, recipe generation has no degraded mode to fall back to
        val repository = buildRepositoryThatThrows(RuntimeException("no network"))

        runBlocking {
            val result = repository.generateRecipes(listOf("Patatas"), 1)

            assertTrue("Result must be Error but was: $result", result is Result.Error)
            assertEquals("no network", (result as Result.Error).exception.message)
        }
    }

    @Test
    fun should_includeRequestedServingsInPrompt_when_generatingRecipes() {
        // R5: the prompt sent to the AI model must state the requested servings count
        val responseJson = """
            [
              {"title":"Tortilla de patatas","difficulty":"Fácil","durationMinutes":30,"servings":4,
               "ingredients":[{"name":"Patatas","quantity":"4 unidades"}],
               "steps":["Cocinar"]}
            ]
        """.trimIndent()
        val (repository, mockModel) = buildRepositoryWithModel(responseJson)

        runBlocking {
            repository.generateRecipes(listOf("Patatas"), 3)

            val promptCaptor = argumentCaptor<String>()
            verifyBlocking(mockModel) { generateContent(promptCaptor.capture()) }

            assertTrue(
                "R5: el prompt debe indicar el número de raciones solicitado",
                promptCaptor.firstValue.contains("Servings required for every recipe: 3")
            )
        }
    }

    @Test
    fun should_overrideServingsWithRequestedValue_when_modelReturnsDifferentServings() {
        // R6: the "servings" field of every mapped Recipe must equal what the user requested,
        // even if the model's JSON response disagrees with the prompt instructions
        val responseJson = """
            [
              {"title":"Tortilla de patatas","difficulty":"Fácil","durationMinutes":30,"servings":2,
               "ingredients":[{"name":"Patatas","quantity":"4 unidades"}],
               "steps":["Cocinar"]},
              {"title":"Ensalada César","difficulty":"Media","durationMinutes":20,"servings":6,
               "ingredients":[{"name":"Lechuga","quantity":"1 unidad"}],
               "steps":["Lavar la lechuga"]},
              {"title":"Paella","difficulty":"Difícil","durationMinutes":60,"servings":1,
               "ingredients":[{"name":"Arroz","quantity":"500g"}],
               "steps":["Cocinar"]}
            ]
        """.trimIndent()
        val repository = buildRepository(responseJson)

        runBlocking {
            val result = repository.generateRecipes(listOf("Patatas"), 4)

            val recipes = (result as Result.Success).data
            assertTrue(recipes.all { it.servings == 4 })
        }
    }
}
