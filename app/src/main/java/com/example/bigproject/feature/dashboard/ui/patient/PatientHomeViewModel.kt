package com.example.bigproject.feature.dashboard.ui.patient

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.usecase.dashboard.GetLastVitalReadingUseCase
import com.example.bigproject.data.healthconnect.HealthConnectAvailability
import com.example.bigproject.data.healthconnect.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientHomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val patientName: String = "Patient",
    val vitals: VitalReading? = null,
    val stressLevel: StressLevel = StressLevel.Calm,
    val healthConnectAvailability: HealthConnectAvailability? = null,
    val isCheckingHealthConnect: Boolean = false,
    val healthConnectMessage: String? = null
)

enum class StressLevel(
    val color: Color,
    val label: String
) {
    Calm(Color(0xFF22C55E), "Calmo"),
    Elevated(Color(0xFFFACC15), "Elevado"),
    High(Color(0xFFEF4444), "Alto")
}

@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    private val getLastVitalReadingUseCase: GetLastVitalReadingUseCase,
    private val authRepository: AuthRepository,
    private val healthConnectManager: HealthConnectManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientHomeUiState())
    val uiState: StateFlow<PatientHomeUiState> = _uiState.asStateFlow()

    val healthPermissionContract = healthConnectManager.requestPermissionsActivityContract()
    val healthConnectPermissions: Set<String> = healthConnectManager.permissions

    init {
        refreshHealthConnectAvailability()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update { it.copy(patientName = user.name) }
            }

            try {
                getLastVitalReadingUseCase().collect { reading ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            vitals = reading,
                            stressLevel = when (reading.stressLevel) {
                                in 0..40 -> StressLevel.Calm
                                in 41..70 -> StressLevel.Elevated
                                else -> StressLevel.High
                            }
                        )
                    }
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

    fun refreshHealthConnectAvailability() {
        viewModelScope.launch {
            loadHealthConnectAvailability()
        }
    }

    fun onHealthPermissionsResult(grantedPermissions: Set<String>) {
        viewModelScope.launch {
            val hasAll = healthConnectPermissions.all { grantedPermissions.contains(it) }
            _uiState.update {
                it.copy(
                    healthConnectMessage = if (hasAll) null else "As permissões do Health Connect são necessárias para sincronizar os dados."
                )
            }
            loadHealthConnectAvailability()
        }
    }

    private suspend fun loadHealthConnectAvailability() {
        _uiState.update { it.copy(isCheckingHealthConnect = true) }
        runCatching { healthConnectManager.getAvailability() }
            .onSuccess { availability ->
                _uiState.update {
                    it.copy(
                        healthConnectAvailability = availability,
                        isCheckingHealthConnect = false
                    )
                }
            }
            .onFailure { throwable ->
                Log.e("PatientHomeViewModel", "Falha ao verificar Health Connect", throwable)
                _uiState.update {
                    it.copy(
                        isCheckingHealthConnect = false,
                        healthConnectAvailability = null,
                        healthConnectMessage = throwable.localizedMessage ?: "Não foi possível verificar o estado do Health Connect."
                    )
                }
            }
    }

    // Trigger manual Health Connect sync directly (no WorkManager), using helper function.
    fun onSyncHealthConnectNow(context: android.content.Context) {
        viewModelScope.launch {
            Log.d("PatientHomeViewModel", "Sync button clicked: starting syncHealthConnectNow")
            val user = authRepository.getCurrentUser()
            val patientId = user?.id
            if (patientId == null) {
                Log.w("PatientHomeViewModel", "No logged user; aborting manual sync")
                return@launch
            }
            try {
                com.example.bigproject.data.healthconnect.syncHealthConnectNow(context, patientId)
                Log.d("PatientHomeViewModel", "syncHealthConnectNow finished successfully")
            } catch (e: Exception) {
                Log.e("PatientHomeViewModel", "syncHealthConnectNow failed", e)
            }
        }
    }
}
