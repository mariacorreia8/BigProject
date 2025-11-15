package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.AlertRepository
import com.example.bigproject.models.StressAlert

class TriggerStressAlertUseCase(
    private val alertRepository: AlertRepository
) {
    suspend operator fun invoke(alert: StressAlert) {
        alertRepository.sendStressAlert(alert)
    }
}
