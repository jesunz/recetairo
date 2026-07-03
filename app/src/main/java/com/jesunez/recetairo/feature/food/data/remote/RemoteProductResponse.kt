package com.jesunez.recetairo.feature.food.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteProductResponse(
    @param:Json(name = "status") val status: Int,
    @param:Json(name = "product") val product: RemoteProduct?
)

@JsonClass(generateAdapter = true)
data class RemoteProduct(
    @param:Json(name = "product_name") val productName: String?,
    @param:Json(name = "image_url") val imageUrl: String?,
    @param:Json(name = "quantity") val quantity: String?,
    @param:Json(name = "categories") val categories: String?
)
