package com.jesunez.recetairo.feature.food.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val expiryDate: String?,   // ISO-8601 (yyyy-MM-dd), null si no aplica
    val imageUrl: String?,
    val insertedAt: Long       // epoch ms
)
