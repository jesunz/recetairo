package com.jesunez.recetairo.feature.recipe.data.db

import androidx.room.TypeConverter
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class RecipeConverters {

    private val moshi = Moshi.Builder().build()
    private val ingredientsAdapter = moshi.adapter<List<IngredientJson>>(
        Types.newParameterizedType(List::class.java, IngredientJson::class.java)
    )
    private val stepsAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    @TypeConverter
    fun fromIngredients(ingredients: List<RecipeIngredient>): String =
        ingredientsAdapter.toJson(ingredients.map { IngredientJson(it.name, it.quantityText) })

    @TypeConverter
    fun toIngredients(json: String): List<RecipeIngredient> =
        ingredientsAdapter.fromJson(json).orEmpty().map { RecipeIngredient(it.name, it.quantityText) }

    @TypeConverter
    fun fromSteps(steps: List<String>): String = stepsAdapter.toJson(steps)

    @TypeConverter
    fun toSteps(json: String): List<String> = stepsAdapter.fromJson(json).orEmpty()
}

@JsonClass(generateAdapter = true)
internal data class IngredientJson(val name: String, val quantityText: String)
