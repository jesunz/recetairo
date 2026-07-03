package com.jesunez.recetairo.core.di

import android.content.Context
import androidx.room.Room
import com.jesunez.recetairo.feature.food.data.dao.FoodDao
import com.jesunez.recetairo.feature.food.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "recetairo.db").build()

    @Provides
    @Singleton
    fun provideFoodDao(db: AppDatabase): FoodDao = db.foodDao()
}
