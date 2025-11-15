package com.example.bigproject.domain.repositories

import com.example.bigproject.models.VitalReading
import kotlinx.coroutines.flow.Flow

interface VitalsRepository {
    fun subscribeToVitals(patientId: String): Flow<VitalReading>
    suspend fun getVitalsHistory(patientId: String): List<VitalReading>
}