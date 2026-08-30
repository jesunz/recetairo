// Feature: recipe-consumer-android, T9*
package com.jesunez.recetairo.feature.recipe.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.data.db.AppDatabase
import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeDifficulty
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeIngredient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RecipeRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RecipeRepositoryImpl(database.recipeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun should_persistRecipeAndReturnItsId_when_savingASingleRecipe() = runBlocking {
        // R19: saving a generated recipe must persist it and make it retrievable by id
        val result = repository.saveRecipe(recipe(title = "Tortilla de patatas"))

        assertTrue(result is Result.Success)
        val id = (result as Result.Success).data
        val saved = (repository.getRecipeById(id).first() as Result.Success).data
        assertEquals("Tortilla de patatas", saved?.title)
    }

    @Test
    fun should_persistAllRecipes_when_savingMultipleRecipesAtOnce() = runBlocking {
        // R20: bulk-saving the recipes checked in Recetas_Generadas must persist all of them
        val result = repository.saveRecipes(
            listOf(recipe(title = "Tortilla de patatas"), recipe(title = "Ensalada de tomate"))
        )

        assertTrue(result is Result.Success)
        val saved = (repository.getSavedRecipes().first() as Result.Success).data
        assertEquals(setOf("Tortilla de patatas", "Ensalada de tomate"), saved.map { it.title }.toSet())
    }

    @Test
    fun should_removeRecipeFromSavedList_when_removingByItsId() = runBlocking {
        // R24: unmarking a recipe as favorite must remove it from Room
        val id = (repository.saveRecipe(recipe(title = "Tortilla de patatas")) as Result.Success).data

        val result = repository.removeRecipe(id)

        assertTrue(result is Result.Success)
        val remaining = (repository.getSavedRecipes().first() as Result.Success).data
        assertTrue(remaining.none { it.id == id })
        val removed = (repository.getRecipeById(id).first() as Result.Success).data
        assertNull(removed)
    }

    private fun recipe(title: String): Recipe = Recipe(
        title = title,
        difficulty = RecipeDifficulty.FACIL,
        durationMinutes = 30,
        servings = 2,
        ingredients = listOf(RecipeIngredient(name = "Huevo", quantityText = "2 uds")),
        steps = listOf("Batir", "Cocinar")
    )
}
