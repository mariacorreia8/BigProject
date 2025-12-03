package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.entities.VitalReading
import com.example.bigproject.domain.repositories.PatientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLastVitalReadingUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(): Flow<VitalReading> {
        // For now, we'll return a mock flow of data.
        // In the future, this will come from the repository.
        return patientRepository.getLatestVitals()
    }
}