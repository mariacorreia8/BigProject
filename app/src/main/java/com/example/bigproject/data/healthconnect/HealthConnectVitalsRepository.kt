package com.example.bigproject.data.healthconnect

import com.example.bigproject.data.model.VitalReading
import java.time.Instant
import javax.inject.Inject

class HealthConnectVitalsRepository @Inject constructor(
    private val manager: HealthConnectManager
) {
    suspend fun getLatestVitals(since: Instant): List<VitalReading> {
        return manager.readLatestVitals(since)
    }

    suspend fun getAvailability(): HealthConnectAvailability {
        return manager.getAvailability()
    }
}