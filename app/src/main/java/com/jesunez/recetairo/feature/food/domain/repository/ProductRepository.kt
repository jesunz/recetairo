package com.jesunez.recetairo.feature.food.domain.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo

interface ProductRepository {
    suspend fun getProductByBarcode(barcode: String): Result<ProductInfo>
}
