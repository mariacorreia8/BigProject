package com.example.bigproject.core.data.repositories

import com.example.bigproject.core.domain.model.user.Patient
import com.example.bigproject.feature.qr.ui.PatientValidationResult
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.repository.NurseHomeRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class ResolveQrTokenRequest(val qrToken: String)

@Serializable
private data class NursePatientsResponse(val patients: List<Patient> = emptyList())

class NurseHomeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository,
    private val apiBaseUrl: String
) : NurseHomeRepository {

    override suspend fun getPatients(): List<Patient> {
        val token = authRepository.getToken() ?: return emptyList()
        val currentUser = authRepository.getCurrentUser() ?: return emptyList()

        return try {
            val response: NursePatientsResponse = httpClient.get("$apiBaseUrl/nurses/${currentUser.id}/patients") {
                bearerAuth(token)
            }.body()

            response.patients
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun resolveQrToken(qrToken: String): Result<PatientValidationResult> {
        return try {
            val nurseAuthToken = authRepository.getToken()
                ?: return Result.failure(Exception("Nurse not authenticated"))

            val response = httpClient.post("$apiBaseUrl/nurse/resolve-qr-token") {
                contentType(ContentType.Application.Json)
                bearerAuth(nurseAuthToken)
                setBody(ResolveQrTokenRequest(qrToken))
            }

            if (response.status.value in 200..299) {
                val responseBody = response.bodyAsText()
                val validationResult =
                    Json.decodeFromString<PatientValidationResult>(responseBody)
                Result.success(validationResult)
            } else {
                Result.failure(Exception("Failed to resolve QR token: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}