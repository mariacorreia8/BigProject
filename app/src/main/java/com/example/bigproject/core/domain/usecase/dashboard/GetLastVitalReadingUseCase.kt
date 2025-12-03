package com.example.bigproject.core.domain.usecase.dashboard

import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.repository.PatientRepository
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
            patientRepository.getLatestVitals(currentUser.id)
        } else {
            // If there's no current user, return an empty flow or handle the error appropriately
            emptyFlow()
        }
    }
}