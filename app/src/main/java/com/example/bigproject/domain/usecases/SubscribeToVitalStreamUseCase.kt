package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.VitalsRepository
import com.example.bigproject.models.VitalReading
import kotlinx.coroutines.flow.Flow

class SubscribeToVitalsStreamUseCase(
    private val vitalsRepository: VitalsRepository
) {
    operator fun invoke(patientId: String): Flow<VitalReading> {
        return vitalsRepository.subscribeToVitals(patientId)
    }
}