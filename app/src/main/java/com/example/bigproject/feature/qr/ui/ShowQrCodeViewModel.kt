package com.example.bigproject.feature.qr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShowQrCodeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionToken: String? = null
)

@HiltViewModel
class ShowQrCodeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowQrCodeUiState())
    val uiState: StateFlow<ShowQrCodeUiState> = _uiState.asStateFlow()

    init {
        createSessionToken()
    }

    private fun createSessionToken() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.createSessionToken()
                .onSuccess {
                    _uiState.update { uiState -> uiState.copy(isLoading = false, sessionToken = it) }
                }
                .onFailure {
                    _uiState.update { uiState ->
                        uiState.copy(
                            isLoading = false,
                            error = it.message ?: "An unknown error occurred"
                        )
                    }
                }
        }
    }
}
