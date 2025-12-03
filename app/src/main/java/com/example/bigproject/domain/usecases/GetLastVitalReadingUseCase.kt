package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.entities.VitalReading
import com.example.bigproject.domain.repositories.AuthRepository
import com.example.bigproject.domain.repositories.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

class GetLastVitalReadingUseCase @Inject constructor(
    private val patientRepository: PatientRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Flow<VitalReading> {
        val currentUser = authRepository.getCurrentUser()
        return if (currentUser != null) {
            patientRepository.getLatestVitals(currentUser.uid)
        } else {
            // If there's no current user, return an empty flow or handle the error appropriately
            emptyFlow()
        }
    }
}
