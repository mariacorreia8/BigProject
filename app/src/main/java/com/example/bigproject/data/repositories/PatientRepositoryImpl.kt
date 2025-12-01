package com.example.bigproject.data.repositories

import com.example.bigproject.domain.entities.AppUser
import com.example.bigproject.domain.entities.Patient
import com.example.bigproject.domain.entities.VitalReading
import com.example.bigproject.domain.repositories.PatientRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

class PatientRepositoryImpl @Inject constructor() : PatientRepository {
    override suspend fun getLatestVitals(): Flow<VitalReading> = flow {
        while (true) {
            val reading = VitalReading(
                heartRate = Random.nextInt(60, 110),
                spo2 = Random.nextInt(95, 100),
                stressLevel = Random.nextInt(10, 80)
            )
            emit(reading)
            delay(5000) // Emit a new reading every 5 seconds
        }
    }

    override suspend fun getPatientByToken(token: String): AppUser? {
        return if (token == "12345") {
            Patient(
                uid = "user-123",
                name = "Carlos",
                email = "carlos@example.com"
            )
        } else {
            null
        }
    }
}