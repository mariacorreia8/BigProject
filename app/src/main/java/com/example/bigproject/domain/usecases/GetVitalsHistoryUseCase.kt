package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.VitalsRepository
import com.example.bigproject.models.VitalReading

class GetVitalsHistoryUseCase(
    private val vitalsRepository: VitalsRepository
) {
    suspend operator fun invoke(patientId: String): List<VitalReading> {
        return vitalsRepository.getVitalsHistory(patientId)
    }
}