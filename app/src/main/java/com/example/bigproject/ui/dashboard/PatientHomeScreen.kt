package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.bigproject.domain.entities.VitalReading


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    viewModel: PatientHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
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

            // Action buttons
            PatientActionsSection(
                onScanPillsClick = { /* TODO */ },
                onDigitalTwinClick = { /* TODO */ },
                onHistoryClick = { /* TODO */ }
            )
        }
    }
}

@Composable
fun VitalsSummaryCard(
    vitals: VitalReading?,
    isLoading: Boolean
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
    onHistoryClick: () -> Unit
) {
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
