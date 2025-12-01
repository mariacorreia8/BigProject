package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.entities.StressAlert
import com.example.bigproject.domain.repositories.AlertRepository

class TriggerStressAlertUseCase(
    private val alertRepository: AlertRepository
) {
    suspend operator fun invoke(alert: StressAlert) {
        alertRepository.sendStressAlert(alert)
    }
}