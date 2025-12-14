package com.example.bigproject.core.domain.repository

import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.model.Medication
import com.example.bigproject.core.domain.model.VitalReading
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun getLatestVitals(patientId: String): Flow<VitalReading>
    suspend fun getVitalsHistory(patientId: String): Flow<List<VitalReading>>
    suspend fun getMedications(patientId: String): Flow<List<Medication>>
    suspend fun getPatientByToken(token: String): AppUser?
}
