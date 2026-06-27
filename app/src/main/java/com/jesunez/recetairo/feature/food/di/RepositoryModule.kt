package com.jesunez.recetairo.feature.food.di

import com.jesunez.recetairo.feature.food.data.repository.FoodRepositoryImpl
import com.jesunez.recetairo.feature.food.data.repository.ProductRepositoryImpl
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.repository.ProductRepository
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

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    // @Binds for OcrRepositoryImpl (T18) will be added when the implementation is created.
}
