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
            VitalsSummaryCard(vitals = uiState.vitals, isLoading = uiState.isLoading)

            Spacer(Modifier.height(16.dp))

            // Stress Chip
            StressChip(level = uiState.stressLevel)

            Spacer(Modifier.height(24.dp))

            // Health Connect Status
            HealthConnectStatusCard(
                availability = uiState.healthConnectAvailability,
                isChecking = uiState.isCheckingHealthConnect,
                onRequestPermissions = { permissionLauncher.launch(viewModel.healthConnectPermissions) },
                onOpenHealthConnect = { openHealthConnectOrStore(context) },
                onRefresh = { viewModel.refreshHealthConnectAvailability() }
            )

            Spacer(Modifier.height(16.dp))

            // Action buttons
            PatientActionsSection(
                onScanPillsClick = { /* TODO */ },
                onDigitalTwinClick = { /* TODO */ },
                onHistoryClick = { /* TODO */ },
                onShowQrClick = { navController.navigate("patient/show_qr_code") },
                onSyncHealthConnectClick = {
                    when (uiState.healthConnectAvailability) {
                        HealthConnectAvailability.InstalledAndAvailable -> viewModel.onSyncHealthConnectNow(context)
                        HealthConnectAvailability.PermissionsNotGranted -> permissionLauncher.launch(viewModel.healthConnectPermissions)
                        HealthConnectAvailability.NotInstalled -> openHealthConnectOrStore(context)
                        HealthConnectAvailability.NotSupported -> viewModel.refreshHealthConnectAvailability()
                        null -> viewModel.refreshHealthConnectAvailability()
                    }
                }
            )
        }
    }
}

@Composable
fun VitalsSummaryCard(
    vitals: VitalReading?,
    isLoading: Boolean = false,
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
    onSyncHealthConnectClick: () -> Unit
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

        // Botão de Sincronização Health Connect
        Button(
            onClick = onSyncHealthConnectClick,
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
                text = "Sincronizar Health Connect",
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
fun HealthConnectStatusCard(
    availability: HealthConnectAvailability?,
    isChecking: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onRefresh: () -> Unit
) {
    if (availability == HealthConnectAvailability.InstalledAndAvailable && !isChecking) {
        return
    }

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
            val primaryActionLabel: String
            val primaryAction: () -> Unit
            val secondaryActionLabel: String?
            val secondaryAction: (() -> Unit)?

            when {
                isChecking -> {
                    title = "A verificar Health Connect"
                    body = "Estamos a verificar o estado do Health Connect..."
                    primaryActionLabel = "Aguarde"
                    primaryAction = {}
                    secondaryActionLabel = null
                    secondaryAction = null
                }

                availability == HealthConnectAvailability.NotInstalled -> {
                    title = "Instale o Health Connect"
                    body = "É necessário instalar o Health Connect para sincronizar os sinais vitais."
                    primaryActionLabel = "Abrir Play Store"
                    primaryAction = onOpenHealthConnect
                    secondaryActionLabel = "Recarregar"
                    secondaryAction = onRefresh
                }

                availability == HealthConnectAvailability.PermissionsNotGranted -> {
                    title = "Permissões necessárias"
                    body = "Conceda acesso ao Health Connect para lermos o seu ritmo cardíaco, SpO₂ e HRV."
                    primaryActionLabel = "Dar permissões"
                    primaryAction = onRequestPermissions
                    secondaryActionLabel = "Recarregar"
                    secondaryAction = onRefresh
                }

                availability == HealthConnectAvailability.NotSupported -> {
                    title = "Health Connect indisponível"
                    body = "Este dispositivo não suporta o Health Connect."
                    primaryActionLabel = "Mais tarde"
                    primaryAction = {}
                    secondaryActionLabel = "Recarregar"
                    secondaryAction = onRefresh
                }

                else -> {
                    title = "Health Connect"
                    body = "Estado desconhecido."
                    primaryActionLabel = "Recarregar"
                    primaryAction = onRefresh
                    secondaryActionLabel = null
                    secondaryAction = null
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
            } else {
                Button(
                    onClick = primaryAction,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking
                ) {
                    Text(primaryActionLabel)
                }
                secondaryActionLabel?.let {
                    TextButton(
                        onClick = { secondaryAction?.invoke() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(it)
                    }
                }
            }
        }
    }
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
