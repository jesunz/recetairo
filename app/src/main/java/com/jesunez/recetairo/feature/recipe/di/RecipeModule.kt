package com.jesunez.recetairo.feature.recipe.di

import com.jesunez.recetairo.feature.recipe.data.repository.FirebaseAiRecipeGenerationRepositoryImpl
import com.jesunez.recetairo.feature.recipe.data.repository.RecipeRepositoryImpl
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeGenerationRepository
import com.jesunez.recetairo.feature.recipe.domain.repository.RecipeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecipeModule {

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindRecipeGenerationRepository(
        impl: FirebaseAiRecipeGenerationRepositoryImpl,
    ): RecipeGenerationRepository
}
