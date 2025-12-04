package com.example.bigproject.core.data.repositories

import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.repository.MessagingRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) : MessagingRepository {

    private val apiBaseUrl = "http://10.0.2.2:5001/bigproject-4a536/us-central1/api"

    override suspend fun registerToken(token: String) {
        val bearer = authRepository.getToken() ?: return
        httpClient.post("$apiBaseUrl/messaging/token") {
            bearerAuth(bearer)
            contentType(ContentType.Application.Json)
            setBody(mapOf("token" to token))
        }
    }
}

