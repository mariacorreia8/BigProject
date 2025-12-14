package com.example.bigproject.core.data.repositories

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.model.user.Patient
import com.example.bigproject.core.domain.model.user.Nurse
import com.example.bigproject.core.domain.model.user.UserRole
import com.example.bigproject.core.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SessionTokenResponse(val token: String)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val httpClient: HttpClient
) : AuthRepository {
    private val apiBaseUrl = "http://10.0.2.2:5001/bigproject-4a536/us-central1/api"
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secret_shared_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    override fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    override suspend fun createSessionToken(): Result<String> {
        return try {
            val authToken = getToken() ?: return Result.failure(Exception("User not authenticated"))
            val response = httpClient.post("https://$apiBaseUrl/patient-session/token") {
                bearerAuth(authToken)
            }
            if (response.status.value in 200..299) {
                val responseBody = response.bodyAsText()
                val sessionToken = Json.decodeFromString<SessionTokenResponse>(responseBody).token
                Result.success(sessionToken)
            } else {
                Result.failure(Exception("Failed to create session token: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun saveUser(user: AppUser) {
        sharedPreferences.edit()
            .putString("user_id", user.id)
            .putString("user_name", user.name)
            .putString("user_email", user.email)
            .putString("user_role", user.role.name)
            .apply()
    }

    override suspend fun getCurrentUser(): AppUser? {
        val id = sharedPreferences.getString("user_id", null) ?: return null
        val name = sharedPreferences.getString("user_name", "") ?: ""
        val email = sharedPreferences.getString("user_email", "") ?: ""
        val roleName = sharedPreferences.getString("user_role", null)

        return when (roleName) {
            UserRole.Patient.name -> Patient(id, name, email)
            UserRole.Nurse.name -> Nurse(id, name, email)
            else -> null
        }
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
