// AuthViewModel.kt
package com.example.bigproject.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // === ESTADOS DE LOGIN ===
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    // === ESTADOS DE REGISTO ===
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    // === DADOS DO UTILIZADOR (nome + role) ===
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    // Modelo de dados do utilizador
    data class UserData(
        val name: String,
        val role: String
    )

    // === FUNÇÃO: LOGIN ===
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                fetchUserData() // Busca nome e role
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Falha no login")
            }
        }
    }

    // === FUNÇÃO: REGISTO ===
    fun register(email: String, password: String, name: String, role: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                // 1. Cria user no Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Falha ao obter UID")

                // 2. Cria documento no Firestore
                val userData = hashMapOf(
                    "id" to uid,
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users").document(uid).set(userData).await()

                // 3. Atualiza estado local
                _userData.value = UserData(name, role)
                _registerState.value = RegisterState.Success
                _loginState.value = LoginState.Success // Login automático
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Falha no registo")
            }
        }
    }

    // === FUNÇÃO: BUSCAR DADOS DO USER DO FIRESTORE ===
    fun fetchUserData() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val name = doc.getString("name") ?: "Utilizador"
                    val role = doc.getString("role") ?: "Patient"
                    _userData.value = UserData(name, role)
                }
            } catch (e: Exception) {
                // Em caso de erro, mantém userData nulo
            }
        }
    }

    // === FUNÇÃO: LOGOUT ===
    fun logout() {
        auth.signOut()
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
        _userData.value = null
    }

    // === VERIFICA SE ESTÁ LOGADO ===
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // === RESET ESTADOS (opcional) ===
    fun resetStates() {
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }
}

// === ESTADOS DE LOGIN ===
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

// === ESTADOS DE REGISTO ===
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}