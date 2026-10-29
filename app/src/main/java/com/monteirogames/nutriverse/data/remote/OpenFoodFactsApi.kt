package com.monteirogames.nutriverse.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OffProductResponse(
    @Json(name = "status") val status: String?,
    @Json(name = "status_verbose") val statusVerbose: String?,
    @Json(name = "product") val product: OffProduct?
)

@JsonClass(generateAdapter = true)
data class OffProduct(
    @Json(name = "id") val barcode: String?,
    @Json(name = "product_name") val productName: String?,
    @Json(name = "brands") val brands: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "serving_size") val servingSize: String?,
    @Json(name = "ingredients_text") val ingredientsText: String?,
    @Json(name = "allergens") val allergens: String?,
    @Json(name = "nutriscore_grade") val nutriscoreGrade: String?,
    @Json(name = "nutriments") val nutriments: OffNutriments?
)

@JsonClass(generateAdapter = true)
data class OffNutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double?,
    @Json(name = "energy-kcal_serving") val energyKcalServing: Double?,
    @Json(name = "proteins_100g") val proteins100g: Double?,
    @Json(name = "proteins_serving") val proteinsServing: Double?,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double?,
    @Json(name = "carbohydrates_serving") val carbohydratesServing: Double?,
    @Json(name = "fat_100g") val fat100g: Double?,
    @Json(name = "fat_serving") val fatServing: Double?
)

interface OpenFoodFactsApi {
    @GET("api/v3/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Header("User-Agent") userAgent: String = "NutriVerse - Android - Version 1.0",
        @Query("fields") fields: String = "id,product_name,brands,image_url,serving_size,ingredients_text,allergens,nutriscore_grade,nutriments"
    ): OffProductResponse
}
