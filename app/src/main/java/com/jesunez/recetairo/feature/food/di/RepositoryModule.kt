package com.jesunez.recetairo.feature.food.di

import com.jesunez.recetairo.feature.food.data.repository.FoodRepositoryImpl
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    // @Binds for ProductRepositoryImpl (T17), OcrRepositoryImpl (T18)
    // will be added when each implementation is created.
}
