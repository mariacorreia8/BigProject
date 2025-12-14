package com.example.bigproject.core.domain.usecase.pillscanner

import com.example.bigproject.core.domain.model.PillScanResult
import com.example.bigproject.core.domain.repository.PillScannerRepository

class ScanPillUseCase(
    private val pillScannerRepository: PillScannerRepository
) {
    suspend operator fun invoke(imagePath: String): PillScanResult {
        return pillScannerRepository.scanPill(imagePath)
    }
}