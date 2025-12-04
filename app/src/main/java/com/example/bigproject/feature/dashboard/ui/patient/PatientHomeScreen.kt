package com.example.bigproject.feature.dashboard.ui.patient

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.bigproject.core.domain.model.VitalReading
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.bigproject.data.healthconnect.HealthConnectAvailability


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    navController: NavController,
    viewModel: PatientHomeViewModel = hiltViewModel(),
    onLogoutClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.healthPermissionContract,
        onResult = { granted -> viewModel.onHealthPermissionsResult(granted) }
    )

    val onHealthConnectAction = remember(uiState.healthConnectAction, uiState.healthConnectAvailability) {
        {
            viewModel.onHealthConnectCtaClicked(
                launchPermissions = { permissionLauncher.launch(it) },
                context = context
            )
        }
    }

    LaunchedEffect(uiState.healthConnectMessage) {
        uiState.healthConnectMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Greeting
            Text(
                text = "Olá, ${uiState.patientName}",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Aqui está o resumo da sua saúde hoje",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(24.dp))

            // Vitals
            VitalsSummaryCard(
                vitals = uiState.vitals,
                isLoading = uiState.isLoading,
                timestampLabel = uiState.latestVitalsTimestampLabel
            )

            Spacer(Modifier.height(16.dp))

            // Stress Chip
            StressChip(level = uiState.stressLevel)

            Spacer(Modifier.height(24.dp))

            // Action buttons
            PatientActionsSection(
                onScanPillsClick = { /* TODO */ },
                onDigitalTwinClick = { /* TODO */ },
                onHistoryClick = { /* TODO */ },
                onShowQrClick = { navController.navigate("patient/show_qr_code") },
                healthConnectAvailability = uiState.healthConnectAvailability,
                isCheckingHealthConnect = uiState.isCheckingHealthConnect,
                healthConnectCtaLabel = uiState.healthConnectCtaLabel,
                healthConnectCtaEnabled = uiState.isHealthConnectCtaEnabled,
                onHealthConnectCtaClick = onHealthConnectAction,
                shouldShowHealthConnectInfo = uiState.shouldShowHealthConnectInfo
            )
        }

        if (uiState.stressAlertDialog.visible) {
            StressAlertDialog(
                state = uiState.stressAlertDialog,
                onDismiss = viewModel::dismissStressDialog,
                onBreathingExercise = {
                    viewModel.startBreathingExercise()
                    navController.navigate("patient/breathing")
                }
            )
        }
    }
}

@Composable
fun VitalsSummaryCard(
    vitals: VitalReading?,
    isLoading: Boolean = false,
    timestampLabel: String = "",
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Última Leitura",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (timestampLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timestampLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VitalItem(
                        label = "HR",
                        value = vitals?.heartRate?.toString() ?: "--",
                        unit = "bpm",
                        modifier = Modifier.weight(1f)
                    )
                    VitalItem(
                        label = "SpO₂",
                        value = vitals?.spo2?.toString() ?: "--",
                        unit = "%",
                        modifier = Modifier.weight(1f)
                    )
                    VitalItem(
                        label = "Stress",
                        value = vitals?.stressLevel?.toString() ?: "--",
                        unit = "",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun VitalItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF22C55E)
        )
        if (unit.isNotBlank()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun StressChip(level: StressLevel) {
    Surface(
        color = level.color,
        shape = RoundedCornerShape(50),
        shadowElevation = 4.dp
    ) {
        Text(
            text = "Stress: ${level.label}",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PatientActionsSection(
    onScanPillsClick: () -> Unit,
    onDigitalTwinClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onShowQrClick: () -> Unit,
    healthConnectAvailability: HealthConnectAvailability?,
    isCheckingHealthConnect: Boolean,
    healthConnectCtaLabel: String,
    healthConnectCtaEnabled: Boolean,
    onHealthConnectCtaClick: () -> Unit,
    shouldShowHealthConnectInfo: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PatientActionButton(
                text = "Escanear",
                modifier = Modifier.weight(1f),
                onClick = onScanPillsClick
            )
            PatientActionButton(
                text = "Gêmeo Digital",
                modifier = Modifier.weight(1f),
                onClick = onDigitalTwinClick
            )
            PatientActionButton(
                text = "Histórico",
                modifier = Modifier.weight(1f),
                onClick = onHistoryClick
            )
        }
        PatientActionButton(
            text = "Compartilhar com Enfermeiro",
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowQrClick
        )

        if (shouldShowHealthConnectInfo || isCheckingHealthConnect) {
            HealthConnectStatusInfo(
                availability = healthConnectAvailability,
                isChecking = isCheckingHealthConnect
            )
        }

        // Botão de Sincronização Health Connect
        Button(
            onClick = onHealthConnectCtaClick,
            enabled = healthConnectCtaEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Sincronizar",
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = healthConnectCtaLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun PatientActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun HealthConnectStatusInfo(
    availability: HealthConnectAvailability?,
    isChecking: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val title: String
            val body: String
            when {
                isChecking -> {
                    title = "A verificar Health Connect"
                    body = "Estamos a verificar o estado do Health Connect..."
                }
                availability == HealthConnectAvailability.NotInstalled -> {
                    title = "Instale o Health Connect"
                    body = "É necessário instalar o Health Connect para sincronizar os sinais vitais."
                }
                availability == HealthConnectAvailability.PermissionsNotGranted -> {
                    title = "Permissões necessárias"
                    body = "Conceda acesso ao Health Connect para lermos o seu ritmo cardíaco, SpO₂ e HRV."
                }
                availability == HealthConnectAvailability.NotSupported -> {
                    title = "Health Connect indisponível"
                    body = "Este dispositivo não suporta o Health Connect."
                }
                else -> {
                    title = "Health Connect"
                    body = "Estado desconhecido."
                }
            }

            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)

            if (isChecking) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun StressAlertDialog(
    state: StressAlertDialogState,
    onDismiss: () -> Unit,
    onBreathingExercise: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Stress elevado") },
        text = {
            Column {
                Text(text = state.message)
                Spacer(Modifier.height(8.dp))
                Text(text = "Severidade: ${state.severity}")
            }
        },
        confirmButton = {
            Button(onClick = onBreathingExercise) {
                Text(text = "Exercício de respiração")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dispensar")
            }
        }
    )
}

private fun openHealthConnectOrStore(context: android.content.Context) {
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
