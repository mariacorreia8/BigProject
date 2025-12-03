package com.example.bigproject.feature.dashboard.ui.nurse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.repository.NurseHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientUiState(
    val id: String,
    val name: String,
    val lastVitals: String,
    val lastAlert: String
)

data class NurseHomeSummaryUiState(
    val totalPatients: Int = 0,
    val alerts: Int = 0
)

data class NurseHomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val summary: NurseHomeSummaryUiState = NurseHomeSummaryUiState(),
    val patients: List<PatientUiState> = emptyList()
)

@HiltViewModel
class NurseHomeViewModel @Inject constructor(
    private val nurseHomeRepository: NurseHomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NurseHomeUiState())
    val uiState: StateFlow<NurseHomeUiState> = _uiState.asStateFlow()

    init {
        loadNurseHomeData()
    }

    private fun loadNurseHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Using hardcoded data for now to populate the UI
                val patientUiStates = listOf(
                    PatientUiState("1", "João Silva", "PA: 120/80, FC: 75", "Sem novos alertas"),
                    PatientUiState("2", "Maria Santos", "PA: 130/85, FC: 80", "Medicação em atraso"),
                    PatientUiState("3", "Carlos Pereira", "PA: 110/70, FC: 65", "Sem novos alertas"),
                    PatientUiState("4", "Ana Ferreira", "PA: 140/90, FC: 90", "Frequência cardíaca alta"),
                    PatientUiState("5", "Pedro Rodrigues", "PA: 125/82, FC: 78", "Sem novos alertas")
                )

                val alertCount = patientUiStates.count { it.lastAlert != "Sem novos alertas" }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        patients = patientUiStates,
                        summary = NurseHomeSummaryUiState(
                            totalPatients = patientUiStates.size,
                            alerts = alertCount
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ocorreu um erro desconhecido"
                    )
                }
            }
        }
    }
}
