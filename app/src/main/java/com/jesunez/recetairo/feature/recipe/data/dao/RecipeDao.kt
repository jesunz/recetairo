package com.jesunez.recetairo.feature.recipe.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jesunez.recetairo.feature.recipe.data.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY savedAt DESC")
    fun getAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    fun search(query: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getById(id: Long): Flow<RecipeEntity?>

    @Insert
    suspend fun insert(entity: RecipeEntity): Long

    @Insert
    suspend fun insertAll(entities: List<RecipeEntity>): List<Long>

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
