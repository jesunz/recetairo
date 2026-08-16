// Feature: insertar-alimentos-despensa, Property 3, Property 6
package com.jesunez.recetairo.feature.food.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.data.db.AppDatabase
import com.jesunez.recetairo.feature.food.domain.model.Food
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FoodRepositoryImplPropertyTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FoodRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FoodRepositoryImpl(database.foodDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun should_returnSuccess_when_insertingFoodWithAnyValidName() {
        // R8: insertFood must succeed for any food with a non-empty name
        runBlocking {
            checkAll(100, Arb.string(1..100)) { name ->
                database.clearAllTables()
                val result = repository.insertFood(Food(name = name, quantity = 1.0))
                assertTrue("R8: insertFood must return Success for name='$name'", result is Result.Success)
            }
        }
    }

    @Test
    fun should_findInsertedFoodByName_when_searchingWithExactName() {
        // R9: after a successful insertion, the food's name must appear in search results
        runBlocking {
            checkAll(100, Arb.string(1..100)) { name ->
                database.clearAllTables()
                repository.insertFood(Food(name = name, quantity = 1.0))
                val results = repository.searchFoodNames(name).first()
                assertTrue(
                    "R9: name must appear in search results after insertion",
                    results.contains(name)
                )
            }
        }
    }

    @Test
    fun should_createTwoDistinctRows_when_insertingSameNameTwice() {
        // R29: pantry allows multiple foods with the same name (different lots/units)
        runBlocking {
            checkAll(100, Arb.string(1..100)) { name ->
                database.clearAllTables()
                val result1 = repository.insertFood(Food(name = name, quantity = 1.0))
                val result2 = repository.insertFood(Food(name = name, quantity = 2.0))
                assertTrue("R29: first insertion must succeed for name='$name'", result1 is Result.Success)
                assertTrue("R29: second insertion must succeed for name='$name'", result2 is Result.Success)
                val allFoods = (repository.getAllFoods().first() as Result.Success).data
                assertEquals(
                    "R29: two rows with the same name must both exist in the pantry",
                    2,
                    allFoods.count { it.name == name }
                )
            }
        }
    }

    @Test
    fun should_returnEachNameOnce_when_sameNameInsertedTwice() {
        // Autocomplete suggestions must not show the same name repeated per matching row
        runBlocking {
            checkAll(100, Arb.string(1..100)) { name ->
                database.clearAllTables()
                repository.insertFood(Food(name = name, quantity = 1.0))
                repository.insertFood(Food(name = name, quantity = 2.0))
                val results = repository.searchFoodNames(name).first()
                assertEquals(
                    "searchFoodNames must not repeat a name already present in the results",
                    1,
                    results.count { it == name }
                )
            }
        }
    }
}
