package com.jesunez.recetairo.feature.recipe.ui

import com.jesunez.recetairo.feature.recipe.domain.model.Recipe
import com.jesunez.recetairo.feature.recipe.domain.model.RecipeMatch

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val match: RecipeMatch? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val error: String? = null
) {
    // R25: texto compartible (título, ingredientes, pasos), vacío hasta que la Receta cargue.
    // Derivado de `recipe` en vez de un campo mutable aparte, mismo criterio que
    // IngredientSelectionUiState.isGenerateEnabled: no hay forma de que quede desincronizado.
    val shareText: String
        get() {
            val currentRecipe = recipe ?: return ""
            return buildString {
                appendLine(currentRecipe.title)
                appendLine()
                appendLine("Ingredientes:")
                currentRecipe.ingredients.forEach { appendLine("- ${it.name} (${it.quantityText})") }
                appendLine()
                appendLine("Pasos:")
                currentRecipe.steps.forEachIndexed { index, step -> appendLine("${index + 1}. $step") }
            }.trim()
        }
}
