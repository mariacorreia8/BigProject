package com.example.bigproject.domain.entities

data class PillScanResult(
    val imageLocalPath: String,
    val detectedImprint: String?,
    val detectedColor: String?,
    val detectedShape: String?,
    val candidateMedications: List<Medication>
)
