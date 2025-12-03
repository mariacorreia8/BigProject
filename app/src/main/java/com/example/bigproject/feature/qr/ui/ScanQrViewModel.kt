package com.example.bigproject.feature.qr.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.repository.NurseHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

data class ScanQrUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val validationResult: PatientValidationResult? = null
)

@Serializable
data class PatientValidationResult(
    val patientId: String,
    val patientName: String
)

@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val nurseHomeRepository: NurseHomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanQrUiState())
    val uiState = _uiState.asStateFlow()

    fun onQrCodeScanned(qrCodeValue: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, validationResult = null) }

            val uri = Uri.parse(qrCodeValue)
            val token = uri.getQueryParameter("token")

            if (token == null) {
                _uiState.update { it.copy(isLoading = false, error = "Código QR inválido.") }
                return@launch
            }

            nurseHomeRepository.resolveQrToken(token)
                .onSuccess {
                    _uiState.update { uiState ->
                        uiState.copy(isLoading = false, validationResult = it)
                    }
                }
                .onFailure {
                    _uiState.update { uiState ->
                        uiState.copy(
                            isLoading = false,
                            error = it.message ?: "Ocorreu um erro desconhecido"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
