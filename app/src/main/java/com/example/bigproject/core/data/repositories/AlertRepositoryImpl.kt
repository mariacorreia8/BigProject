package com.example.bigproject.core.data.repositories

import com.example.bigproject.core.domain.model.StressAlert
import com.example.bigproject.core.domain.repository.AlertRepository
import com.example.bigproject.core.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) : AlertRepository {

    private val apiBaseUrl = "http://10.0.2.2:5001/bigproject-4a536/us-central1/api"

    override suspend fun sendStressAlert(alert: StressAlert) {
        val token = authRepository.getToken() ?: return
        httpClient.post("$apiBaseUrl/patients/${alert.patientId}/alerts") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "severity" to alert.severity,
                "message" to alert.message
            ))
        }
    }

    override fun getStressAlerts(patientId: String): Flow<List<StressAlert>> = flow {
        val token = authRepository.getToken() ?: return@flow
        val response: AlertsResponse = httpClient.get("$apiBaseUrl/patients/$patientId/alerts") {
            bearerAuth(token)
        }.body()
        emit(response.alerts)
    }
}

@kotlinx.serialization.Serializable
private data class AlertsResponse(val alerts: List<StressAlert>)
