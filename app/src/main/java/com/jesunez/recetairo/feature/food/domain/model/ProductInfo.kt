package com.jesunez.recetairo.feature.food.domain.model

data class ProductInfo(
    val barcode: String,
    val name: String?,
    val brand: String?,
    val category: String?,
    val imageUrl: String?
)
