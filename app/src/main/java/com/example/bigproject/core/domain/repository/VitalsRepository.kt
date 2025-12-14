package com.example.bigproject.core.domain.repository

import com.example.bigproject.core.domain.model.VitalReading
import kotlinx.coroutines.flow.Flow

interface VitalsRepository {
    fun subscribeToVitals(patientId: String): Flow<VitalReading>
    suspend fun getVitalsHistory(patientId: String): List<VitalReading>
}