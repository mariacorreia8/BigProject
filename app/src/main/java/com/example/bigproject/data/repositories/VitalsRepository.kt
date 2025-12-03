package com.example.bigproject.data.repositories

import com.example.bigproject.data.healthconnect.HealthConnectManager
import com.example.bigproject.models.VitalReading
import java.time.Instant
import javax.inject.Inject

class VitalsRepository @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    suspend fun getLatestVitals(since: Instant): List<VitalReading> {
        return healthConnectManager.readLatestVitals(since)
    }
}