package com.example.bigproject.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.model.user.Nurse
import com.example.bigproject.core.domain.model.user.Patient
import com.example.bigproject.core.domain.model.user.UserRole
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.repository.MessagingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val messagingRepository: MessagingRepository
) : ViewModel() {


    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    private val _userData = MutableStateFlow<AppUser?>(null)
    val userData: StateFlow<AppUser?> = _userData

    init {
        viewModelScope.launch {
            // Try to get from Firebase Auth first, fallback to stored user
            _userData.value = authRepository.getCurrentFirebaseUser() 
                ?: authRepository.getCurrentUser()
        }
    }

    fun getIdToken(): String? = authRepository.getToken()

    fun register(email: String, password: String, name: String, role: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            val result = authRepository.registerWithEmailAndPassword(email, password, name, role)
            result.getOrElse { error ->
                _registerState.value = RegisterState.Error(error.message ?: "Falha no registo")
                return@launch
            }
            val user = result.getOrNull()!!
            _userData.value = user
            registerMessagingToken()
            _registerState.value = RegisterState.Success
            _loginState.value = LoginState.Success
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.loginWithEmailAndPassword(email, password)
            result.getOrElse { error ->
                _loginState.value = LoginState.Error(error.message ?: "Falha no login")
                return@launch
            }
            val user = result.getOrNull()!!
            _userData.value = user
            registerMessagingToken()
            _loginState.value = LoginState.Success
        }
    }

    fun logout() {
        authRepository.signOut()
        _userData.value = null
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }

    fun isUserLoggedIn(): Boolean = authRepository.getToken() != null

    private fun registerMessagingToken() {
        viewModelScope.launch {
            authRepository.getMessagingToken()?.let {
                messagingRepository.registerToken(it)
            }
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