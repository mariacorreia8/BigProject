package com.example.bigproject.feature.dashboard.data

import com.example.bigproject.core.domain.model.Medication
import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.model.user.Patient
import com.example.bigproject.core.domain.repository.PatientRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

class PatientRepositoryImpl @Inject constructor() : PatientRepository {
    // Corrected to accept patientId
    override suspend fun getLatestVitals(patientId: String): Flow<VitalReading> = flow {
        // patientId is unused in this mock implementation
        while (true) {
            val reading = VitalReading(
                heartRate = Random.Default.nextInt(60, 110),
                spo2 = Random.Default.nextInt(95, 100),
                stressLevel = Random.Default.nextInt(10, 80)
            )
            emit(reading)
            delay(5000) // Emit a new reading every 5 seconds
        }
    }

    // Implemented missing method
    override suspend fun getVitalsHistory(patientId: String): Flow<List<VitalReading>> = flow {
        // patientId is unused in this mock implementation
        val history = (0..20).map {
            VitalReading(
                heartRate = Random.Default.nextInt(60, 110),
                spo2 = Random.Default.nextInt(95, 100),
                stressLevel = Random.Default.nextInt(10, 80)
            )
        }
        emit(history)
    }

    // Implemented missing method
    override suspend fun getMedications(patientId: String): Flow<List<Medication>> = flow {
        // patientId is unused in this mock implementation
        // For now, returning an empty list. You can add mock medications here.
        emit(emptyList<Medication>())
    }

    override suspend fun getPatientByToken(token: String): AppUser? {
        return if (token == "12345") {
            Patient(
                id = "user-123",
                name = "Carlos",
                email = "carlos@example.com"
            )
        } else {
            null
        }
    }
}