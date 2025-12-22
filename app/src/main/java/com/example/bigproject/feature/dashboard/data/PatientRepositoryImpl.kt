package com.example.bigproject.feature.dashboard.data

import com.example.bigproject.core.domain.model.Medication
import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.model.user.Patient
import com.example.bigproject.core.domain.repository.PatientRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

private const val VITALS_COLLECTION = "vital_readings"

class PatientRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PatientRepository {
    override suspend fun getLatestVitals(patientId: String): Flow<VitalReading> = callbackFlow {
        val registration = firestore.collection(VITALS_COLLECTION)
            .whereEqualTo("patientId", patientId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reading = snapshot?.documents?.firstOrNull()?.toVitalReading()
                if (reading != null) trySend(reading).isSuccess
            }

        awaitClose { registration.remove() }
    }

    override suspend fun getVitalsHistory(patientId: String): Flow<List<VitalReading>> = flow {
        val snapshot = firestore.collection(VITALS_COLLECTION)
            .whereEqualTo("patientId", patientId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        emit(snapshot.documents.mapNotNull { it.toVitalReading() })
    }

    override suspend fun getMedications(patientId: String): Flow<List<Medication>> = flow {
        emit(emptyList())
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

private fun com.google.firebase.firestore.DocumentSnapshot.toVitalReading(): VitalReading? {
    val data = data ?: return null
    val heartRate = data["heartRate"].toIntOrNull() ?: 0
    val spo2 = data["spo2"].toIntOrNull() ?: 0
    val stress = data["stressLevel"].toIntOrNull() ?: estimateStressLevel(heartRate, data["heartRateVariability"].toDoubleOrNull())
    return VitalReading(
        heartRate = heartRate,
        spo2 = spo2,
        stressLevel = stress,
        id = id,
        patientId = data["patientId"] as? String ?: "",
        timestamp = data["timestamp"].toLongOrNull() ?: System.currentTimeMillis(),
        bodyBattery = data["bodyBattery"].toIntOrNull() ?: 0,
        deviceSource = data["deviceSource"] as? String ?: "HealthConnect"
    )
}

private fun Any?.toIntOrNull(): Int? = when (this) {
    is Int -> this
    is Long -> toInt()
    is Double -> toInt()
    else -> null
}

private fun Any?.toLongOrNull(): Long? = when (this) {
    is Long -> this
    is Int -> toLong()
    is Double -> toLong()
    else -> null
}

private fun Any?.toDoubleOrNull(): Double? = when (this) {
    is Double -> this
    is Float -> toDouble()
    is Long -> toDouble()
    is Int -> toDouble()
    else -> null
}

private fun estimateStressLevel(heartRate: Int, heartRateVariability: Double?): Int {
    if (heartRate == 0 && heartRateVariability == null) return 0
    val normalizedHr = heartRate.coerceIn(50, 140)
    val hrvPenalty = (100 - (heartRateVariability ?: 50.0)).coerceIn(0.0, 100.0)
    return (((normalizedHr - 50) / 90.0) * 60 + hrvPenalty * 0.4).toInt().coerceIn(0, 100)
}
