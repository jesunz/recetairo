package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.repository.ProductRepository
import javax.inject.Inject

class LookupProductByBarcodeUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): Result<ProductInfo> = try {
        productRepository.getProductByBarcode(barcode)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
