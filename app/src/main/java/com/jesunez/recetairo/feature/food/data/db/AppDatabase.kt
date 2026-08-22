package com.jesunez.recetairo.feature.food.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jesunez.recetairo.feature.food.data.dao.FoodDao
import com.jesunez.recetairo.feature.food.data.entity.FoodEntity
import com.jesunez.recetairo.feature.recipe.data.dao.RecipeDao
import com.jesunez.recetairo.feature.recipe.data.db.RecipeConverters
import com.jesunez.recetairo.feature.recipe.data.entity.RecipeEntity

@Database(entities = [FoodEntity::class, RecipeEntity::class], version = 4, exportSchema = false)
@TypeConverters(DateConverters::class, RecipeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN emoji TEXT")
            }
        }

        // Category summaries (GetCategorySummariesUseCase) tolerate unrecognized `category`
        // values and fold them into "Otros", but getByCategory filters with an exact match.
        // Rows saved before that normalization existed (empty string, raw external taxonomy
        // text, casing variants) inflated the "Otros" count without showing up in its list.
        // Fold them into the canonical label here so both paths agree.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE foods SET category = 'Otros'
                    WHERE category NOT IN (
                        'Lácteos', 'Carne', 'Pescado', 'Marisco',
                        'Frutas', 'Verduras', 'Pan', 'Cereales', 'Otros'
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recipes (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        servings INTEGER NOT NULL,
                        ingredientsJson TEXT NOT NULL,
                        stepsJson TEXT NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
