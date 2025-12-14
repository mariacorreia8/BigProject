package com.example.bigproject.core.domain.repository

import com.example.bigproject.core.domain.model.user.Patient
import com.example.bigproject.feature.qr.ui.PatientValidationResult

interface NurseHomeRepository {
    suspend fun getPatients(): List<Patient>
    suspend fun resolveQrToken(qrToken: String): Result<PatientValidationResult>
}