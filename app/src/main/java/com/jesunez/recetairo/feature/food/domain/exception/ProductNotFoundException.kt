package com.jesunez.recetairo.feature.food.domain.exception

class ProductNotFoundException(barcode: String) : Exception("Product not found for barcode: $barcode")
