package com.example.bigproject.models

data class VitalReading(
    val id: String = "",
    val patientId: String = "",
    val timestamp: Long,
    val heartRate: Int?,
    val heartRateVariability: Double?,
    val spo2: Double?,
    val deviceSource: String
)
