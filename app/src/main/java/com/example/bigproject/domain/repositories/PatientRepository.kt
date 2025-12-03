package com.example.bigproject.domain.repositories

import com.example.bigproject.domain.entities.AppUser
import com.example.bigproject.domain.entities.VitalReading
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun getLatestVitals(): Flow<VitalReading>
    suspend fun getPatientByToken(token: String): AppUser?
}