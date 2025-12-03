package com.example.bigproject.data.repositories

import com.example.bigproject.domain.entities.StressAlert
import com.example.bigproject.domain.repositories.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor() : AlertRepository {
    override suspend fun sendStressAlert(alert: StressAlert) {
        // In a real implementation, this would send the alert to a remote server.
        // For now, we'll just print it to the console.
        println("Sending stress alert: $alert")
    }

    override suspend fun getStressAlerts(patientId: String): Flow<List<StressAlert>> = flow {
        // patientId is unused in this mock implementation
        val alerts = listOf(
            StressAlert(
                id = "1",
                patientId = patientId,
                timestamp = System.currentTimeMillis() - 10000,
                severity = 8,
                message = "High stress detected after lunch.",
                acknowledged = false
            ),
            StressAlert(
                id = "2",
                patientId = patientId,
                timestamp = System.currentTimeMillis() - 200000,
                severity = 6,
                message = "Moderate stress during morning meeting.",
                acknowledged = true
            )
        )
        emit(alerts)
    }
}
