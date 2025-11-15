package com.example.bigproject.domain.repositories

import com.example.bigproject.models.StressAlert

interface AlertRepository {
    suspend fun sendStressAlert(alert: StressAlert)
}