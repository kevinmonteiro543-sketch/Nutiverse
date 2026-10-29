package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.remote.OpenFoodFactsApi
import com.monteirogames.nutriverse.domain.model.ScannedProduct
import com.monteirogames.nutriverse.domain.repository.BarcodeRepository
import retrofit2.HttpException
import java.io.IOException

class BarcodeRepositoryImpl(
    private val api: OpenFoodFactsApi
) : BarcodeRepository {
    override suspend fun getProductByBarcode(barcode: String): Result<ScannedProduct> {
        return try {
            val response = api.getProduct(barcode)
            if ((response.status == "success" || response.status == "1") && response.product != null) {
                val p = response.product
                val n = p.nutriments
                
                val kcal = n?.energyKcalServing ?: n?.energyKcal100g
                val pro = n?.proteinsServing ?: n?.proteins100g
                val carb = n?.carbohydratesServing ?: n?.carbohydrates100g
                val fat = n?.fatServing ?: n?.fat100g

                val scannedProduct = ScannedProduct(
                    barcode = barcode,
                    name = p.productName ?: "Unknown Product",
                    brand = p.brands,
                    imageUrl = p.imageUrl,
                    servingSize = p.servingSize,
                    ingredients = p.ingredientsText,
                    allergens = p.allergens,
                    nutriscore = p.nutriscoreGrade,
                    energyKcal = kcal?.toFloat(),
                    protein = pro?.toFloat(),
                    carbs = carb?.toFloat(),
                    fat = fat?.toFloat()
                )
                Result.success(scannedProduct)
            } else {
                Result.failure(Exception("Product not found for barcode: $barcode"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
