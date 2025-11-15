package com.example.bigproject.models

data class StressAlert(
    val id: String,
    val patientId: String,
    val timestamp: Long,
    val severity: Int,
    val message: String,
    val acknowledged: Boolean
)
