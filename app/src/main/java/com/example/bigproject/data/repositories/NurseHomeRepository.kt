
package com.example.bigproject.data.repositories

import com.example.bigproject.models.Patient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface NurseHomeRepository {
    suspend fun getPatients(): List<Patient>
}

class NurseHomeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NurseHomeRepository {

    override suspend fun getPatients(): List<Patient> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "Patient")
                .get()
                .await()
            snapshot.toObjects(Patient::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
