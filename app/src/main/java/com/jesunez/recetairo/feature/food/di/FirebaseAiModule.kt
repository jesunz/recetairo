package com.jesunez.recetairo.feature.food.di

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseAiModule {

    private const val MODEL_NAME = "gemini-2.5-flash-lite"

    @Provides
    @Singleton
    fun provideFoodExtractionGenerativeModel(): GenerativeModel {
        val foodItemSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(),
                "quantity" to Schema.string(),
                "category" to Schema.enumeration(FoodCategory.entries.map { it.label() })
            ),
            optionalProperties = listOf("quantity")
        )

        return Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.array(foodItemSchema)
            }
        )
    }
}
