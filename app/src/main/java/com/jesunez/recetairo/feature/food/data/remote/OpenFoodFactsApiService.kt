package com.jesunez.recetairo.feature.food.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApiService {
    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(@Path("barcode") barcode: String): RemoteProductResponse
}
