// ui/auth/AuthViewModel.kt
package com.example.bigproject.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.ktor.client.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.bigproject.models.VitalReading
import com.google.firebase.firestore.FieldValue


class AuthViewModel(
    private val client: HttpClient // Ktor client for API calls
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // === ESTADOS ===
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    data class UserData(val name: String, val role: String)

    init {
        // Load user data if already logged in
        loadCurrentUserData()
    }

    // === CARREGAR DADOS DO UTILIZADOR ATUAL ===
    private fun loadCurrentUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                fetchUserDataFromFirestore(currentUser.uid)
            }
        }
    }

    // === BUSCAR DADOS DO UTILIZADOR NO FIRESTORE ===
    private suspend fun fetchUserDataFromFirestore(uid: String) {
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val name = doc.getString("name") ?: ""
                val role = doc.getString("role") ?: ""
                _userData.value = UserData(name, role)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // === REGISTO COM FIREBASE AUTH ===
    fun register(email: String, password: String, name: String, role: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                // Criar utilizador no Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("User creation failed")
                val uid = user.uid

                // Criar documento no Firestore com name e role
                val userData = mapOf(
                    "id" to uid,
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(uid).set(userData).await()

                // Carregar dados do utilizador
                _userData.value = UserData(name, role)
                _registerState.value = RegisterState.Success
                _loginState.value = LoginState.Success // login automático após registo
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Falha no registo")
            }
        }
    }

    // === LOGIN COM FIREBASE AUTH ===
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                if (user != null) {
                    fetchUserDataFromFirestore(user.uid)
                }
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Falha no login")
            }
        }
    }

    // === LOGOUT ===
    fun logout() {
        auth.signOut()
        _userData.value = null
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }

    // === OBTER ID TOKEN PARA API CALLS ===
    suspend fun getIdToken(): String? {
        return try {
            val tokenResult = auth.currentUser?.getIdToken(false)?.await()
            tokenResult?.token
        } catch (e: Exception) {
            null
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null



    // === BUSCAR PACIENTE POR EMAIL (via Firestore) ===
    suspend fun getPatientByEmail(email: String): PatientData? {
        return try {
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

// === MODELOS ===
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
