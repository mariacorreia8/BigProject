package com.example.bigproject.core.domain.usecase.dashboard

import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow

class SubscribeToVitalsStreamUseCase(
    private val vitalsRepository: VitalsRepository
) {
    operator fun invoke(patientId: String): Flow<VitalReading> {
        return vitalsRepository.subscribeToVitals(patientId)
    }
}