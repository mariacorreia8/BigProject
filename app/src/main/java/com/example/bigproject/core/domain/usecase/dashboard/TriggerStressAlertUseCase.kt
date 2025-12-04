package com.example.bigproject.core.domain.usecase.dashboard

import com.example.bigproject.core.domain.model.StressAlert
import com.example.bigproject.core.domain.repository.AlertRepository
import javax.inject.Inject

class TriggerStressAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository
) {
    suspend operator fun invoke(alert: StressAlert) {
        alertRepository.sendStressAlert(alert)
    }
}