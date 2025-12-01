package com.example.bigproject.domain.repositories

import com.example.bigproject.domain.entities.PillScanResult


interface PillScannerRepository {
    suspend fun scanPill(imagePath: String): PillScanResult
}