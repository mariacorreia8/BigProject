package com.example.bigproject.data

import android.util.Log
import com.example.bigproject.data.healthconnect.HealthConnectVitalsRepository
import com.example.bigproject.data.healthconnect.HealthConnectAvailability
import com.example.bigproject.data.firebase.FirebaseVitalsRepository
import com.example.bigproject.models.VitalReading
import java.time.Instant
import javax.inject.Inject

class VitalsRepository @Inject constructor(
    private val healthRepo: HealthConnectVitalsRepository,
    private val fbRepo: FirebaseVitalsRepository
) {

    suspend fun getLatestVitals(since: Instant): List<VitalReading> {
        return healthRepo.getLatestVitals(since)
    }

    suspend fun getHealthConnectAvailability(): HealthConnectAvailability {
        return healthRepo.getAvailability()
    }

    suspend fun upsertVitals(vitals: List<VitalReading>) {
        fbRepo.saveVitals(vitals)
    }
    suspend fun syncLastestVitals(patientId: String, since: Instant){
        // Read from Health Connect
        val localReadings = getLatestVitals(since)
        // Enrich with patientId and deviceSource
        val enriched = localReadings.map {
            it.copy(
                patientId = patientId,
                deviceSource = it.deviceSource.ifEmpty { "HealthConnect" }
            )
        }
        //Upsert to Firebase
        upsertVitals(enriched)
        Log.d("VitalsRepository", "Sincronizadas ${enriched.size} leituras.")
    }
}

