package com.jesunez.recetairo.feature.recipe.di

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RecipeGenerativeModel

@Module
@InstallIn(SingletonComponent::class)
object FirebaseAiRecipeModule {

    private const val MODEL_NAME = "gemini-3.5-flash-lite"
    private const val RECIPE_COUNT = 3

    @Provides
    @Singleton
    @RecipeGenerativeModel
    fun provideRecipeGenerativeModel(): GenerativeModel {
        val ingredientSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(),
                "quantity" to Schema.string()
            )
        )

        val recipeSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(),
                "difficulty" to Schema.enumeration(RecipeDifficulty.entries.map { it.label() }),
                "durationMinutes" to Schema.integer(),
                "servings" to Schema.integer(),
                "ingredients" to Schema.array(ingredientSchema),
                "steps" to Schema.array(Schema.string())
            )
        )

        return Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.array(
                    recipeSchema,
                    minItems = RECIPE_COUNT,
                    maxItems = RECIPE_COUNT
                )
            }
        )
    }
}
