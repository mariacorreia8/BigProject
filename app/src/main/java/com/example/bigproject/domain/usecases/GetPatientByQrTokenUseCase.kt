package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.PatientRepository
import com.example.bigproject.models.User

class GetPatientByQrTokenUseCase(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(token: String): User? =
        patientRepository.getPatientByToken(token)
}
