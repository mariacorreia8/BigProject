// ui/auth/AuthViewModel.kt
package com.example.bigproject.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import com.example.bigproject.models.VitalReading
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val client: HttpClient // Ktor client
) : ViewModel() {

    private val apiBaseUrl = "http://10.0.2.2:5001/bigproject-4a536/us-central1/api"

    // === ESTADOS ===
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    init {
        _userData.value = authRepository.getUser()
    }

    fun getIdToken(): String? = authRepository.getToken()

    data class UserData(val name: String, val role: String)

    // === REGISTO VIA API ===
    fun register(email: String, password: String, name: String, role: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                val response: ApiAuthResponse = client.post("$apiBaseUrl/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf(
                        "email" to email,
                        "password" to password,
                        "name" to name,
                        "role" to role
                    ))
                }.body()

                authRepository.saveToken(response.idToken)
                authRepository.saveUser(response.user.name, response.user.role)
                _userData.value = UserData(response.user.name, response.user.role)
                _registerState.value = RegisterState.Success
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Falha no registo")
            }
        }
    }

    // === LOGIN VIA API ===
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response: ApiAuthResponse = client.post("$apiBaseUrl/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to email, "password" to password))
                }.body()

                authRepository.saveToken(response.idToken)
                authRepository.saveUser(response.user.name, response.user.role)
                _userData.value = UserData(response.user.name, response.user.role)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Falha no login")
            }
        }
    }

    // === LOGOUT ===
    fun logout() {
        authRepository.clear()
        _userData.value = null
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }

    fun isUserLoggedIn(): Boolean = authRepository.getToken() != null



    // === BUSCAR PACIENTE POR EMAIL (via Firestore local) ===
    suspend fun getPatientByEmail(email: String): PatientData? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "Patient")
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents[0]
                PatientData(
                    name = doc.getString("name") ?: "Paciente",
                    email = doc.getString("email") ?: email,
                    uid = doc.id
                )
            } else null
        } catch (e: Exception) { null }
    }

    // === BUSCAR ÚLTIMA LEITURA VITAL ===
    suspend fun getLatestVitalReading(patientId: String): VitalReading? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("vital_readings")
                .whereEqualTo("patientId", patientId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents[0]
                VitalReading(
                    id = doc.id,
                    patientId = doc.getString("patientId") ?: patientId,
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    heartRate = doc.getLong("heartRate")?.toInt() ?: 0,
                    spo2 = doc.getLong("spo2")?.toInt() ?: 0,
                    stressLevel = doc.getLong("stressLevel")?.toInt() ?: 0,
                    bodyBattery = doc.getLong("bodyBattery")?.toInt() ?: 0,
                    deviceSource = doc.getString("deviceSource") ?: "Desconhecido"
                )
            } else null
        } catch (e: Exception) { null }
    }
}

// === MODELOS DA API ===
@Serializable
data class ApiAuthResponse(
    val idToken: String,
    val user: ApiUserData
)

@Serializable
data class ApiUserData(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class PatientData(
    val name: String,
    val email: String,
    val uid: String
)

// === ESTADOS ===
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}