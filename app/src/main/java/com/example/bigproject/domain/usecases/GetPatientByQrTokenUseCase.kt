package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.entities.AppUser
import com.example.bigproject.domain.repositories.PatientRepository

class GetPatientByQrTokenUseCase(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(token: String): AppUser? =
        patientRepository.getPatientByToken(token)
}