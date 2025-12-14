package com.example.bigproject.core.domain.repository

import com.example.bigproject.core.domain.model.PillScanResult


interface PillScannerRepository {
    suspend fun scanPill(imagePath: String): PillScanResult
}