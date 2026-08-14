package com.jesunez.recetairo.navigation

import com.jesunez.recetairo.feature.food.domain.model.PantryFilter
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import java.net.URLEncoder

private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")

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
    }

    object Pantry : Screen(
        "pantry?category={category}&expiringSoon={expiringSoon}"
    ) {
        const val BASE_ROUTE = "pantry"

        fun buildRoute(filter: PantryFilter): String = when (filter) {
            PantryFilter.All -> BASE_ROUTE
            is PantryFilter.ByCategory -> "$BASE_ROUTE?category=${filter.category.name.encode()}"
            PantryFilter.ExpiringSoon -> "$BASE_ROUTE?expiringSoon=true"
        }
    }

    object FoodDetail : Screen("food_detail/{foodId}") {
        const val BASE_ROUTE = "food_detail"

        fun buildRoute(foodId: Long): String = "$BASE_ROUTE/$foodId"
    }
}
