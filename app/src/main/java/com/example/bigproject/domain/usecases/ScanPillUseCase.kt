package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.PillScannerRepository
import com.example.bigproject.models.PillScanResult

class ScanPillUseCase(
    private val pillScannerRepository: PillScannerRepository
) {
    suspend operator fun invoke(imagePath: String): PillScanResult {
        return pillScannerRepository.scanPill(imagePath)
    }
}

