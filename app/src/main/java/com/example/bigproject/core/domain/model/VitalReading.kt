package com.example.bigproject.core.domain.model

data class VitalReading(
    val heartRate: Int,
    val spo2: Int,
    val stressLevel: Int,
    val id: String = "",
    val patientId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val bodyBattery: Int = 0,
    val deviceSource: String = ""
)