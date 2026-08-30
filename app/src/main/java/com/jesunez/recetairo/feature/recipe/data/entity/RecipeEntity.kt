package com.jesunez.recetairo.feature.recipe.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val difficulty: String,
    val durationMinutes: Int,
    val servings: Int,
    val ingredientsJson: List<RecipeIngredient>,
    val stepsJson: List<String>,
    val savedAt: Long // epoch ms
)
