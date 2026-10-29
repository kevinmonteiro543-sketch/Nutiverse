package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.ScannedProduct
import com.monteirogames.nutriverse.domain.repository.BarcodeRepository

class GetProductFromBarcodeUseCase(private val repository: BarcodeRepository) {
    suspend operator fun invoke(barcode: String): Result<ScannedProduct> {
        return repository.getProductByBarcode(barcode)
    }
}

data class ScannerUseCases(
    val getProductFromBarcode: GetProductFromBarcodeUseCase
)
