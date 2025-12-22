package com.example.bigproject.feature.dashboard.ui.nurse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.model.StressAlert
import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.repository.AlertRepository
import com.example.bigproject.core.domain.repository.NurseHomeRepository
import com.example.bigproject.core.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private val nurseHomeRepository: NurseHomeRepository,
    private val patientRepository: PatientRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(NurseHomeUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<NurseHomeUiState> = _uiState.asStateFlow()

    init {
        loadNurseHomeData()
    }

    private fun loadNurseHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val patients = nurseHomeRepository.getPatients()

                val patientUiStates = patients.map { patient ->
                    val latestVitals = runCatching {
                        patientRepository.getLatestVitals(patient.id).firstOrNull()
                    }.getOrNull()

                    val latestAlert = runCatching {
                        alertRepository.getStressAlerts(patient.id).firstOrNull()?.maxByOrNull { it.timestamp }
                    }.getOrNull()

                    PatientUiState(
                        id = patient.id,
                        name = patient.name,
                        lastVitals = latestVitals.toCardVitalsString(),
                        lastAlert = latestAlert.toCardAlertString()
                    )
                }

                val alertCount = patientUiStates.count { it.lastAlert != NO_ALERTS }

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

    private fun VitalReading?.toCardVitalsString(): String {
        if (this == null) return NO_VITALS
        // Keep it compact; UI can format more later.
        return "FC: ${heartRate} | SpO₂: ${spo2} | Stress: ${stressLevel}"
    }

    private fun StressAlert?.toCardAlertString(): String {
        if (this == null) return NO_ALERTS
        val time = runCatching {
            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
        }.getOrNull()
        return if (time != null) "${message} ($time)" else message
    }

    private companion object {
        const val NO_ALERTS = "Sem novos alertas"
        const val NO_VITALS = "Sem vitals"
    }
}
