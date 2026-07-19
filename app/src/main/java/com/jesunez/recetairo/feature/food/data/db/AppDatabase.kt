package com.jesunez.recetairo.feature.food.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jesunez.recetairo.feature.food.data.dao.FoodDao
import com.jesunez.recetairo.feature.food.data.entity.FoodEntity

@Database(entities = [FoodEntity::class], version = 2, exportSchema = false)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN emoji TEXT")
            }
        }
    }
}
