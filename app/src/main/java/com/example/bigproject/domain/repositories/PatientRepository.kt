package com.example.bigproject.domain.repositories

import com.example.bigproject.domain.entities.AppUser
import com.example.bigproject.domain.entities.Medication
import com.example.bigproject.domain.entities.VitalReading
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun getLatestVitals(patientId: String): Flow<VitalReading>
    suspend fun getVitalsHistory(patientId: String): Flow<List<VitalReading>>
    suspend fun getMedications(patientId: String): Flow<List<Medication>>
    suspend fun getPatientByToken(token: String): AppUser?
}
