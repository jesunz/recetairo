package com.jesunez.recetairo.feature.food.data.repository

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.data.remote.OpenFoodFactsApiService
import com.jesunez.recetairo.feature.food.domain.exception.ProductNotFoundException
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.repository.ProductRepository
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

class ProductRepositoryImpl @Inject constructor(
    private val apiService: OpenFoodFactsApiService
) : ProductRepository {

    override suspend fun getProductByBarcode(barcode: String): Result<ProductInfo> = try {
        val response = apiService.getProduct(barcode)
        val name = response.product?.productName?.takeIf { it.isNotBlank() }
        if (response.status == 0 || name == null) {
            // OFF can return status=1 with no usable product_name (e.g. less popular barcodes);
            // treat that the same as "not found" so the user gets the same explicit banner
            // instead of silently landing on an empty-looking form
            Result.Error(ProductNotFoundException(barcode))
        } else {
            val product = response.product
            Result.Success(
                ProductInfo(
                    barcode = barcode,
                    name = name,
                    brand = null,
                    category = product.categories,
                    imageUrl = product.imageUrl
                )
            )
        }
    } catch (e: HttpException) {
        Result.Error(e, "HTTP error ${e.code()}: ${e.message()}")
    } catch (e: IOException) {
        Result.Error(e, "Network error: ${e.message}")
    } catch (e: Exception) {
        Result.Error(e)
    }
}
