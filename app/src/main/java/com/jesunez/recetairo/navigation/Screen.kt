package com.jesunez.recetairo.navigation

import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import java.net.URLEncoder

sealed class Screen(val route: String) {

    object Home : Screen("home")

    object BarcodeScan : Screen("barcode_scan")

    object ReceiptScan : Screen("receipt_scan")

    object AddFood : Screen(
        "add_food_manual?barcode={barcode}&name={name}&brand={brand}&category={category}&imageUrl={imageUrl}"
    ) {
        const val BASE_ROUTE = "add_food_manual"

        fun buildRoute(productInfo: ProductInfo? = null): String {
            if (productInfo == null) return BASE_ROUTE
            val params = listOfNotNull(
                "barcode=${productInfo.barcode.encode()}",
                productInfo.name?.let { "name=${it.encode()}" },
                productInfo.brand?.let { "brand=${it.encode()}" },
                productInfo.category?.let { "category=${it.encode()}" },
                productInfo.imageUrl?.let { "imageUrl=${it.encode()}" }
            )
            return "$BASE_ROUTE?${params.joinToString("&")}"
        }

        private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")
    }
}
