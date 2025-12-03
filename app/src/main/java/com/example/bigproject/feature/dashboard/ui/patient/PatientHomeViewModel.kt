package com.example.bigproject.feature.dashboard.ui.patient

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.usecase.dashboard.GetLastVitalReadingUseCase
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
    val stressLevel: StressLevel = StressLevel.Calm
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientHomeUiState())
    val uiState: StateFlow<PatientHomeUiState> = _uiState.asStateFlow()

    init {
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
}
