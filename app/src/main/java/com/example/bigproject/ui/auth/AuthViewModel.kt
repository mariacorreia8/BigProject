package com.example.bigproject.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.domain.entities.AppUser
import com.example.bigproject.domain.entities.Nurse
import com.example.bigproject.domain.entities.Patient
import com.example.bigproject.domain.entities.UserRole
import com.example.bigproject.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val client: HttpClient
) : ViewModel() {

    private val apiBaseUrl = "http://10.0.2.2:5001/bigproject-4a536/us-central1/api"

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    private val _userData = MutableStateFlow<AppUser?>(null)
    val userData: StateFlow<AppUser?> = _userData

    init {
        viewModelScope.launch {
            _userData.value = authRepository.getCurrentUser()
        }
    }

    fun getIdToken(): String? = authRepository.getToken()

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
                val user = response.user.toAppUser()
                authRepository.saveUser(user)
                _userData.value = user
                _registerState.value = RegisterState.Success
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Falha no registo")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response: ApiAuthResponse = client.post("$apiBaseUrl/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to email, "password" to password))
                }.body()

                authRepository.saveToken(response.idToken)
                val user = response.user.toAppUser()
                authRepository.saveUser(user)
                _userData.value = user
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Falha no login")
            }
        }
    }

    fun logout() {
        authRepository.clear()
        _userData.value = null
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }

    fun isUserLoggedIn(): Boolean = authRepository.getToken() != null
}

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
) {
    fun toAppUser(): AppUser {
        return when (UserRole.valueOf(role)) {
            UserRole.Patient -> Patient(uid = id, name = name, email = email)
            UserRole.Nurse -> Nurse(uid = id, name = name, email = email)
        }
    }
}

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