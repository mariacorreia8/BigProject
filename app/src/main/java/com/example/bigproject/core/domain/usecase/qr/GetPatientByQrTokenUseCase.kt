package com.example.bigproject.core.domain.usecase.qr

import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.repository.PatientRepository

class GetPatientByQrTokenUseCase(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(token: String): AppUser? =
        patientRepository.getPatientByToken(token)
}