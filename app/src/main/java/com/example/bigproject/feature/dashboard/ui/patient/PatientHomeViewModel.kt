package com.example.bigproject.feature.dashboard.ui.patient

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.stress.StressAnalyzer
import com.example.bigproject.core.domain.usecase.dashboard.GetLastVitalReadingUseCase
import com.example.bigproject.core.domain.usecase.dashboard.TriggerStressAlertUseCase
import com.example.bigproject.data.healthconnect.HealthConnectAvailability
import com.example.bigproject.data.healthconnect.HealthConnectManager
import com.example.bigproject.feature.dashboard.stress.StressAlertHandler
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
    val healthConnectMessage: String? = null,
    val stressAlertDialog: StressAlertDialogState = StressAlertDialogState.Hidden,
    val healthConnectCtaLabel: String = "Verificar Health Connect",
    val isHealthConnectCtaEnabled: Boolean = true,
    val healthConnectAction: HealthConnectCtaAction = HealthConnectCtaAction.CheckAvailability,
    val latestVitalsTimestampLabel: String = "",
    val shouldShowHealthConnectInfo: Boolean = true
)

data class StressAlertDialogState(
    val alertId: String = "",
    val message: String = "",
    val severity: Int = 0,
    val visible: Boolean = false
) {
    companion object {
        val Hidden = StressAlertDialogState()
    }
}

enum class StressLevel(
    val color: Color,
    val label: String
) {
    Calm(Color(0xFF22C55E), "Calmo"),
    Elevated(Color(0xFFFACC15), "Elevado"),
    High(Color(0xFFEF4444), "Alto")
}

enum class HealthConnectCtaAction {
    RequestPermissions,
    OpenHealthConnect,
    CheckAvailability,
    Sync
}

@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    private val getLastVitalReadingUseCase: GetLastVitalReadingUseCase,
    private val authRepository: AuthRepository,
    private val healthConnectManager: HealthConnectManager,
    private val stressAnalyzer: StressAnalyzer,
    private val stressAlertHandler: StressAlertHandler,
    private val triggerStressAlertUseCase: TriggerStressAlertUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientHomeUiState())
    val uiState: StateFlow<PatientHomeUiState> = _uiState.asStateFlow()

    val healthPermissionContract = healthConnectManager.requestPermissionsActivityContract()
    val healthConnectPermissions: Set<String> = healthConnectManager.permissions

    private var pendingPermissionResultCallback: ((Set<String>) -> Unit)? = null

    init {
        refreshHealthConnectAvailability()
        observeStressAlerts()
        observeVitals()
    }

    private fun observeVitals() {
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
                            },
                            latestVitalsTimestampLabel = formatVitalsTimestamp(reading.timestamp)
                        )
                    }
                    user?.let {
                        val alert = stressAnalyzer.evaluate(reading, it.id, it.name)
                        if (alert != null) {
                            stressAlertHandler.handle(alert)
                            triggerBackendAlert(alert)
                        }
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

    private fun observeStressAlerts() {
        viewModelScope.launch {
            stressAlertHandler.latestAlert.collect { alert ->
                if (alert != null) {
                    _uiState.update {
                        it.copy(
                            stressAlertDialog = StressAlertDialogState(
                                alertId = alert.id,
                                message = alert.message,
                                severity = alert.severity,
                                visible = true
                            )
                        )
                    }
                }
            }
        }
    }

    fun dismissStressDialog() {
        stressAlertHandler.clear()
        _uiState.update { it.copy(stressAlertDialog = StressAlertDialogState.Hidden) }
    }

    fun startBreathingExercise() {
        // Placeholder hook for actual exercise flow
        dismissStressDialog()
    }

    private fun triggerBackendAlert(alert: com.example.bigproject.core.domain.model.StressAlert) {
        viewModelScope.launch {
            try {
                triggerStressAlertUseCase(alert)
            } catch (e: Exception) {
                Log.e("PatientHomeViewModel", "Falha ao enviar alerta", e)
            }
        }
    }

    fun refreshHealthConnectAvailability() {
        viewModelScope.launch {
            loadHealthConnectAvailability()
        }
    }

    private suspend fun refreshHealthConnectAvailabilityImmediate() {
        _uiState.update { it.copy(isCheckingHealthConnect = true) }
        loadHealthConnectAvailability()
    }

    fun onHealthPermissionsResult(grantedPermissions: Set<String>) {
        viewModelScope.launch {
            val requestedAllGranted = grantedPermissions.containsAll(healthConnectPermissions)
            val hasAll = requestedAllGranted || runCatching { healthConnectManager.hasRequiredPermissions() }
                .onFailure { Log.e("PatientHomeViewModel", "Failed to verify Health Connect permissions", it) }
                .getOrDefault(false)

            if (hasAll) {
                _uiState.update {
                    it.copy(
                        healthConnectMessage = "Permissões concedidas.",
                        healthConnectCtaLabel = "Sincronizar Health Connect",
                        healthConnectAction = HealthConnectCtaAction.Sync,
                        isHealthConnectCtaEnabled = true,
                        shouldShowHealthConnectInfo = false
                    )
                }
                refreshHealthConnectAvailabilityImmediate()
            } else {
                val availability = runCatching { healthConnectManager.getAvailability() }
                    .onFailure { Log.e("PatientHomeViewModel", "Failed to fetch availability after denial", it) }
                    .getOrDefault(HealthConnectAvailability.PermissionsNotGranted)
                val ctaState = buildHealthConnectCtaState(availability)
                val fallbackLabel = if (grantedPermissions.isEmpty()) {
                    "Pedido de permissões recusado."
                } else {
                    "Pedido recusado ou incompleto. Conclua pelo Health Connect."
                }
                _uiState.update {
                    it.copy(
                        healthConnectMessage = fallbackLabel,
                        healthConnectAvailability = availability,
                        healthConnectCtaLabel = ctaState.label,
                        isHealthConnectCtaEnabled = true,
                        healthConnectAction = ctaState.action,
                        shouldShowHealthConnectInfo = true,
                        isCheckingHealthConnect = false
                    )
                }
            }

            pendingPermissionResultCallback?.invoke(grantedPermissions)
            pendingPermissionResultCallback = null
        }
    }

    fun onHealthConnectCtaClicked(launchPermissions: (Set<String>) -> Unit, context: android.content.Context) {
        when (uiState.value.healthConnectAction) {
            HealthConnectCtaAction.RequestPermissions -> launchPermissions(healthConnectPermissions)
            HealthConnectCtaAction.OpenHealthConnect -> openHealthConnect(context)
            HealthConnectCtaAction.CheckAvailability -> refreshHealthConnectAvailability()
            HealthConnectCtaAction.Sync -> onSyncHealthConnectNow(context)
        }
    }

    private fun openHealthConnect(context: android.content.Context) {
        val permissionsIntent = healthConnectManager.buildPermissionsIntent()
        try {
            context.startActivity(permissionsIntent)
            return
        } catch (intentError: Exception) {
            Log.w("PatientHomeViewModel", "Permissions intent failed, fallback to app/store", intentError)
        }

        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
        if (intent != null) {
            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            val marketIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        }
    }

    private suspend fun loadHealthConnectAvailability() {
        _uiState.update {
            it.copy(
                isCheckingHealthConnect = true,
                healthConnectCtaLabel = "A verificar...",
                isHealthConnectCtaEnabled = false,
                healthConnectAction = HealthConnectCtaAction.CheckAvailability,
                shouldShowHealthConnectInfo = shouldShowHealthConnectCard(it.healthConnectAvailability)
            )
        }
        runCatching { healthConnectManager.getAvailability() }
            .onSuccess { availability ->
                _uiState.update {
                    val ctaState = buildHealthConnectCtaState(availability)
                    it.copy(
                        healthConnectAvailability = availability,
                        isCheckingHealthConnect = false,
                        healthConnectCtaLabel = ctaState.label,
                        isHealthConnectCtaEnabled = ctaState.enabled,
                        healthConnectAction = ctaState.action,
                        shouldShowHealthConnectInfo = availability != HealthConnectAvailability.InstalledAndAvailable
                    )
                }
            }
            .onFailure { throwable ->
                Log.e("PatientHomeViewModel", "Falha ao verificar Health Connect", throwable)
                _uiState.update {
                    it.copy(
                        isCheckingHealthConnect = false,
                        healthConnectAvailability = null,
                        healthConnectMessage = throwable.localizedMessage
                            ?: "Não foi possível verificar o estado do Health Connect.",
                        healthConnectCtaLabel = "Tentar novamente",
                        isHealthConnectCtaEnabled = true,
                        healthConnectAction = HealthConnectCtaAction.CheckAvailability,
                        shouldShowHealthConnectInfo = true
                    )
                }
            }
    }

    private fun shouldShowHealthConnectCard(availability: HealthConnectAvailability?): Boolean {
        return availability == null || availability != HealthConnectAvailability.InstalledAndAvailable
    }

    private fun formatVitalsTimestamp(timestamp: Long): String {
        return runCatching {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(formatter)
        }.getOrElse { "" }
    }

    private fun buildHealthConnectCtaState(availability: HealthConnectAvailability?): HealthConnectCtaState {
        return when (availability) {
            HealthConnectAvailability.NotInstalled -> HealthConnectCtaState(
                label = "Instalar Health Connect",
                enabled = true,
                action = HealthConnectCtaAction.OpenHealthConnect
            )
            HealthConnectAvailability.PermissionsNotGranted -> HealthConnectCtaState(
                label = "Dar permissões",
                enabled = true,
                action = HealthConnectCtaAction.RequestPermissions
            )
            HealthConnectAvailability.InstalledAndAvailable -> HealthConnectCtaState(
                label = "Sincronizar Health Connect",
                enabled = true,
                action = HealthConnectCtaAction.Sync
            )
            HealthConnectAvailability.NotSupported -> HealthConnectCtaState(
                label = "Health Connect indisponível",
                enabled = false,
                action = HealthConnectCtaAction.CheckAvailability
            )
            null -> HealthConnectCtaState(
                label = "Verificar Health Connect",
                enabled = true,
                action = HealthConnectCtaAction.CheckAvailability
            )
        }
    }

    private data class HealthConnectCtaState(
        val label: String,
        val enabled: Boolean,
        val action: HealthConnectCtaAction
    )

    // Trigger manual Health Connect sync directly (no WorkManager), using helper function.
    fun onSyncHealthConnectNow(context: android.content.Context) {
        viewModelScope.launch {
            Log.d("PatientHomeViewModel", "Sync button clicked: starting syncHealthConnectNow")
            val user = authRepository.getCurrentUser()
            val patientId = user?.id
            if (patientId == null) {
                Log.w("PatientHomeViewModel", "No logged user; aborting manual sync")
                _uiState.update { it.copy(healthConnectMessage = "Não foi possível identificar o utilizador.") }
                return@launch
            }
            try {
                com.example.bigproject.data.healthconnect.syncHealthConnectNow(context, patientId)
                _uiState.update {
                    it.copy(
                        healthConnectMessage = "Dados sincronizados com sucesso.",
                        shouldShowHealthConnectInfo = false
                    )
                }
                Log.d("PatientHomeViewModel", "syncHealthConnectNow finished successfully")
            } catch (e: Exception) {
                Log.e("PatientHomeViewModel", "syncHealthConnectNow failed", e)
                _uiState.update { it.copy(healthConnectMessage = "Falha ao sincronizar dados: ${e.localizedMessage}") }
            }
        }
    }
}
