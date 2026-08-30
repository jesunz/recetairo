// Feature: recipe-consumer-android, T9*
package com.jesunez.recetairo.feature.recipe.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jesunez.recetairo.feature.food.data.db.AppDatabase
import com.jesunez.recetairo.feature.recipe.data.entity.RecipeEntity
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var recipeDao: RecipeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recipeDao = database.recipeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun should_orderRecipesByMostRecentSavedAtFirst_when_gettingAll() = runBlocking {
        // R16, R28: saved recipes must be listed most recently saved first
        recipeDao.insert(recipeEntity(title = "Más antigua", savedAt = 1_000L))
        recipeDao.insert(recipeEntity(title = "Más reciente", savedAt = 3_000L))
        recipeDao.insert(recipeEntity(title = "Intermedia", savedAt = 2_000L))

        val titles = recipeDao.getAll().first().map { it.title }

        assertEquals(listOf("Más reciente", "Intermedia", "Más antigua"), titles)
    }

    @Test
    fun should_returnOnlyMatchingRecipes_when_searchingByPartialTitle() = runBlocking {
        // R2: search must filter saved recipes by a partial title match
        recipeDao.insert(recipeEntity(title = "Tortilla de patatas", savedAt = 1_000L))
        recipeDao.insert(recipeEntity(title = "Ensalada de tomate", savedAt = 2_000L))
        recipeDao.insert(recipeEntity(title = "Tortilla francesa", savedAt = 3_000L))

        val titles = recipeDao.search("Tortilla").first().map { it.title }

        assertEquals(setOf("Tortilla francesa", "Tortilla de patatas"), titles.toSet())
        assertEquals(2, titles.size)
    }

    @Test
    fun should_returnAllRecipes_when_searchQueryIsEmpty() = runBlocking {
        recipeDao.insert(recipeEntity(title = "Tortilla de patatas", savedAt = 1_000L))
        recipeDao.insert(recipeEntity(title = "Ensalada de tomate", savedAt = 2_000L))

        val titles = recipeDao.search("").first().map { it.title }

        assertEquals(2, titles.size)
    }

    private fun recipeEntity(title: String, savedAt: Long): RecipeEntity = RecipeEntity(
        title = title,
        difficulty = "Fácil",
        durationMinutes = 30,
        servings = 2,
        ingredientsJson = listOf(RecipeIngredient(name = "Huevo", quantityText = "2 uds")),
        stepsJson = listOf("Batir", "Cocinar"),
        savedAt = savedAt
    )
}
