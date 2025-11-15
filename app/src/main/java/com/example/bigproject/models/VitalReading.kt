package com.example.bigproject.models

import java.sql.Timestamp
import javax.xml.transform.Source

data class VitalReading(
    val id: String,
    val patientId: String,
    val timestamp: Long,
    val heartRate: Int,
    val spo2: Int,
    val stressLevel: Int,
    val bodyBattery: Int? = null,
    val deviceSource: String
)
