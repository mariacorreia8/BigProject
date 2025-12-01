package com.example.bigproject.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.models.Patient
import com.example.bigproject.domain.repositories.AuthRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class NurseHomeSummaryUiState(val totalPatients: Int, val alerts: Int)

data class PatientCardUiState(
    val id: String,
    val name: String,
    val lastVitals: String,
    val lastAlert: String
)

data class NurseHomeUiState(
    val summary: NurseHomeSummaryUiState = NurseHomeSummaryUiState(0, 0),
    val patients: List<PatientCardUiState> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NurseHomeViewModel @Inject constructor(
    private val nurseHomeRepository: NurseHomeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NurseHomeUiState())
    val uiState: StateFlow<NurseHomeUiState> = _uiState.asStateFlow()

    init {
        val nurseId = Firebase.auth.currentUser?.uid
        if (nurseId != null) {
            fetchData(nurseId)
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
        }
    }

    private fun fetchData(nurseId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val patients = nurseHomeRepository.getPatientsForNurse(nurseId)

                // TODO: Implement logic to fetch vitals and alerts to calculate summary
                // For now, summary remains 0.

                _uiState.value = _uiState.value.copy(
                    patients = patients,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@Singleton
class NurseHomeRepository @Inject constructor() {
    private val db = Firebase.firestore

    suspend fun getPatientsForNurse(nurseId: String): List<PatientCardUiState> {
        // Query the 'users' collection for patients who have this nurse's ID in their 'nurseIds' list.
        val patientsSnapshot = db.collection("users")
            .whereArrayContains("nurseIds", nurseId)
            .get()
            .await()

        if (patientsSnapshot.isEmpty) {
            return emptyList()
        }

        // Map the user documents to the UI state.
        return patientsSnapshot.documents.mapNotNull { doc ->
            val patient = doc.toObject<Patient>()
            if (patient != null) {
                PatientCardUiState(
                    id = patient.id,
                    name = patient.name,
                    // TODO: Fetch real vitals and alerts
                    lastVitals = "Ainda não implementado",
                    lastAlert = "Ainda não implementado"
                )
            } else {
                null
            }
        }
    }
}