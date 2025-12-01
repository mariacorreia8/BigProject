package com.example.bigproject.domain.repositories

import com.example.bigproject.domain.entities.StressAlert

interface AlertRepository {
    suspend fun sendStressAlert(alert: StressAlert)
}