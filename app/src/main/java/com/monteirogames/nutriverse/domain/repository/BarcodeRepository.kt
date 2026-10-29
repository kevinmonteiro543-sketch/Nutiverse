package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.ScannedProduct

interface BarcodeRepository {
    suspend fun getProductByBarcode(barcode: String): Result<ScannedProduct>
}
