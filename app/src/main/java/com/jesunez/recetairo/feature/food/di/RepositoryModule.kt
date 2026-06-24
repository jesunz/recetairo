package com.jesunez.recetairo.feature.food.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// @Binds for FoodRepositoryImpl (T14), ProductRepositoryImpl (T17), OcrRepositoryImpl (T18)
// will be added when each implementation is created.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule
