package com.example.bigproject.data.repositories

import com.example.bigproject.domain.entities.Patient
import com.example.bigproject.domain.repositories.AuthRepository
import com.example.bigproject.ui.nurse.PatientValidationResult
import com.google.firebase.firestore.FirebaseFirestore
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class ResolveQrTokenRequest(val qrToken: String)

interface NurseHomeRepository {
    suspend fun getPatients(): List<Patient>
    suspend fun resolveQrToken(qrToken: String): Result<PatientValidationResult>
}
private val apiBaseUrl = "http://10.0.2.2:5001/bigproject-4a536/us-central1/api"
class NurseHomeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) : NurseHomeRepository {

    override suspend fun getPatients(): List<Patient> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "Patient")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.getString("uid")?.let {
                    Patient(
                        uid = it,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun resolveQrToken(qrToken: String): Result<PatientValidationResult> {
        return try {
            val nurseAuthToken = authRepository.getToken()
                ?: return Result.failure(Exception("Nurse not authenticated"))

            val response = httpClient.post("https://$apiBaseUrl/nurse/resolve-qr-token") {
                contentType(ContentType.Application.Json)
                bearerAuth(nurseAuthToken)
                setBody(ResolveQrTokenRequest(qrToken))
            }

            if (response.status.value in 200..299) {
                val responseBody = response.bodyAsText()
                val validationResult = Json.decodeFromString<PatientValidationResult>(responseBody)
                Result.success(validationResult)
            } else {
                Result.failure(Exception("Failed to resolve QR token: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
