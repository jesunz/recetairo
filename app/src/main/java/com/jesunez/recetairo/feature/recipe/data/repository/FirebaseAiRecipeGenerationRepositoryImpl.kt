package com.jesunez.recetairo.feature.recipe.data.repository

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.recipe.data.dto.RecipeAiResponseDto
import com.jesunez.recetairo.feature.recipe.data.mapper.toDomain
import com.jesunez.recetairo.feature.recipe.di.RecipeGenerativeModel
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeGenerationRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class FirebaseAiRecipeGenerationRepositoryImpl @Inject constructor(
    @param:RecipeGenerativeModel private val generativeModel: GenerativeModel,
    private val auth: FirebaseAuth,
    private val moshi: Moshi
) : RecipeGenerationRepository {

    override suspend fun generateRecipes(ingredientNames: List<String>): Result<List<Recipe>> = try {
        withTimeout(TIMEOUT_MS.milliseconds) {
            ensureAuthenticated()
            val prompt = PROMPT_TEMPLATE.replace(
                INGREDIENTS_PLACEHOLDER,
                ingredientNames.joinToString(", ")
            )
            val response = generativeModel.generateContent(prompt)
            val json = response.text
                ?: return@withTimeout Result.Error(IllegalStateException("Empty AI response"))

            val adapter = moshi.adapter<List<RecipeAiResponseDto>>(
                Types.newParameterizedType(List::class.java, RecipeAiResponseDto::class.java)
            )
            val dtos = adapter.fromJson(json).orEmpty()

            Result.Success(dtos.toDomain())
        }
    } catch (e: TimeoutCancellationException) {
        Result.Error(e, "Recipe generation timeout: exceeded 30 seconds")
    } catch (e: Exception) {
        Timber.e(e, "Firebase AI recipe generation failed")
        Result.Error(e)
    }

    private suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
        const val INGREDIENTS_PLACEHOLDER = "{{INGREDIENTS}}"

        val PROMPT_TEMPLATE = """
            You are a recipe generator for a Spanish-speaking home cooking app.
            Given a list of ingredients the user already has, propose exactly 3
            distinct recipes, written entirely in Spanish (title, ingredient
            names, and preparation steps).

            STRICT RULES:
            - Generate exactly 3 different recipes. Do not repeat the same dish
              twice, and do not propose minor variations of the same dish.
            - Every recipe MUST use the following ingredients, prioritizing them
              as the main components of the dish: $INGREDIENTS_PLACEHOLDER
            - You may add other common pantry ingredients to complete each
              recipe, but they must be reasonable accompaniments for the
              prioritized ingredients above (e.g. basic spices, oil, salt, rice,
              pasta, common vegetables).
            - For each recipe, provide:
              - "title": a short, appetizing recipe name, in Spanish.
              - "difficulty": exactly one of "Fácil", "Media", "Difícil".
              - "durationMinutes": total preparation plus cooking time, in
                minutes, as a whole number.
              - "servings": number of people the recipe serves, as a whole
                number.
              - "ingredients": the full list of ingredients needed for the
                recipe (both prioritized and additional), each with "name" (in
                Spanish) and "quantity" (a short free-text amount, e.g. "250g",
                "2 unidades").
              - "steps": the preparation steps, in Spanish, in the order they
                must be performed, each step as a single string.
            - Output must be a JSON array of exactly 3 recipe objects matching
              the provided schema. Do not include any text, explanation, or
              markdown outside the JSON array.

            Ingredients to prioritize: $INGREDIENTS_PLACEHOLDER
        """.trimIndent()
    }
}
