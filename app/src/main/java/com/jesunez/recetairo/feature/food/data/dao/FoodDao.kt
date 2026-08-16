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

    @Query("SELECT DISTINCT name FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 5")
    fun searchNames(query: String): Flow<List<String>>

    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun getAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE expiryDate IS NOT NULL AND expiryDate <= :thresholdIso ORDER BY expiryDate ASC")
    fun getExpiringSoon(thresholdIso: String): Flow<List<FoodEntity>>

    @Query("SELECT category, COUNT(*) as itemCount FROM foods GROUP BY category")
    fun getCategoryCounts(): Flow<List<CategoryCountRow>>

    @Query("SELECT * FROM foods WHERE id = :id")
    fun getById(id: Long): Flow<FoodEntity?>

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM foods WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
