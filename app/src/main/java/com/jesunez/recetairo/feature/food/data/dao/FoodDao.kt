package com.jesunez.recetairo.feature.food.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jesunez.recetairo.feature.food.data.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert
    suspend fun insert(entity: FoodEntity): Long

    @Query("SELECT name FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 5")
    fun searchNames(query: String): Flow<List<String>>
}
