package com.example.bigproject.data.firebase

import com.example.bigproject.models.VitalReading
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseVitalsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private companion object {
        const val COLLECTION_VITALS = "vital_readings"
    }

    suspend fun saveVitals(vitals: List<VitalReading>) {
        if (vitals.isEmpty()) return
        val batch = firestore.batch()
        vitals.forEach { vital ->
            val docId = buildDocumentId(vital)
            val docRef = firestore.collection(COLLECTION_VITALS).document(docId)
            batch.set(docRef, vital.toMap())
        }
        batch.commit().await()
    }

    private fun buildDocumentId(vital: VitalReading): String {
        val timestampPart = vital.timestamp.toString()
        val patientPart = if (vital.patientId.isNotBlank()) vital.patientId else "unknown"
        return "${patientPart}_$timestampPart"
    }

    private fun VitalReading.toMap(): Map<String, Any?> = mapOf(
        "patientId" to patientId,
        "timestamp" to timestamp,
        "heartRate" to heartRate,
        "heartRateVariability" to heartRateVariability,
        "spo2" to spo2,
        "deviceSource" to deviceSource
    )
}

