package com.jesunez.recetairo.feature.recipe.domain

import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class GeneratedRecipesCache @Inject constructor() {

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    fun publish(recipes: List<Recipe>) {
        _recipes.value = recipes
    }
}
