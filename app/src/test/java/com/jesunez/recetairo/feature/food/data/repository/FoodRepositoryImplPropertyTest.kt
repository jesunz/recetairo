// Feature: insertar-alimentos-despensa, Property 3
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
}
