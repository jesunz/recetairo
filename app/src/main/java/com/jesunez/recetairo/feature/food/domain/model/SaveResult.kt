package com.jesunez.recetairo.feature.food.domain.model

sealed class SaveResult {
    data object Success : SaveResult()
    data class Failure(val message: String) : SaveResult()
}
