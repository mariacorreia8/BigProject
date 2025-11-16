package com.example.bigproject.models

import java.sql.Timestamp
import javax.xml.transform.Source

data class VitalReading(
    val id: String = "",
    val patientId: String = "",
    val timestamp: Long = 0L,
    val heartRate: Int = 0,
    val spo2: Int = 0,
    val stressLevel: Int = 0,
    val bodyBattery: Int = 0,
    val deviceSource: String = ""
)
