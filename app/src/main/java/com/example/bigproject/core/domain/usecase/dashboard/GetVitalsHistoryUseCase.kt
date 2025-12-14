package com.example.bigproject.core.domain.usecase.dashboard

import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.repository.VitalsRepository

class GetVitalsHistoryUseCase(
    private val vitalsRepository: VitalsRepository
) {
    suspend operator fun invoke(patientId: String): List<VitalReading> {
        return vitalsRepository.getVitalsHistory(patientId)
    }
}