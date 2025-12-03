package com.example.bigproject.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.domain.entities.Medication
import com.example.bigproject.domain.entities.StressAlert
import com.example.bigproject.domain.entities.VitalReading
import com.example.bigproject.domain.repositories.AlertRepository
import com.example.bigproject.domain.repositories.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NursePatientDashboardUiState(
    val latestVitals: VitalReading? = null,
    val vitalsHistory: List<VitalReading> = emptyList(),
    val stressAlerts: List<StressAlert> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NursePatientDashboardViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NursePatientDashboardUiState())
    val uiState: StateFlow<NursePatientDashboardUiState> = _uiState.asStateFlow()

    fun loadPatientData(patientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val latestVitalsFlow = patientRepository.getLatestVitals(patientId)
            val vitalsHistoryFlow = patientRepository.getVitalsHistory(patientId)
            val stressAlertsFlow = alertRepository.getStressAlerts(patientId)
            val medicationsFlow = patientRepository.getMedications(patientId)

            combine(
                latestVitalsFlow,
                vitalsHistoryFlow,
                stressAlertsFlow,
                medicationsFlow
            ) { latestVitals, vitalsHistory, stressAlerts, medications ->
                NursePatientDashboardUiState(
                    latestVitals = latestVitals,
                    vitalsHistory = vitalsHistory,
                    stressAlerts = stressAlerts,
                    medications = medications,
                    isLoading = false
                )
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }
    }
}
