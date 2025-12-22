package com.example.bigproject.core.domain.repository

import com.example.bigproject.core.domain.model.StressAlert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    suspend fun sendStressAlert(alert: StressAlert)
    fun getStressAlerts(patientId: String): Flow<List<StressAlert>>
}
