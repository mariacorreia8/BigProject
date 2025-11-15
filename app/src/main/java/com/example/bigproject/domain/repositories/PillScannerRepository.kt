package com.example.bigproject.domain.repositories

import com.example.bigproject.models.PillScanResult


interface PillScannerRepository {
    suspend fun scanPill(imagePath: String): PillScanResult
}