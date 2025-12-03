package com.example.bigproject.domain.repositories

import com.example.bigproject.domain.entities.StressAlert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    suspend fun sendStressAlert(alert: StressAlert)
    suspend fun getStressAlerts(patientId: String): Flow<List<StressAlert>>
}
