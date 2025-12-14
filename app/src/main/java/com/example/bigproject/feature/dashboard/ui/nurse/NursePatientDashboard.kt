package com.example.bigproject.feature.dashboard.ui.nurse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bigproject.feature.pillscanner.ui.MedicationList
import com.example.bigproject.feature.dashboard.ui.components.StressAlertList
import com.example.bigproject.feature.dashboard.ui.components.TrendChart
import com.example.bigproject.feature.dashboard.ui.patient.VitalsSummaryCard

@Composable
fun NursePatientDashboard(
    patientId: String,
    viewModel: NursePatientDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(patientId) {
        viewModel.loadPatientData(patientId)
    }

    if (uiState.isLoading) {
        CircularProgressIndicator()
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live vitals
            VitalsSummaryCard(vitals = uiState.latestVitals)

            Spacer(modifier = Modifier.height(16.dp))

            // Last 24h history
            TrendChart(
                readings = uiState.vitalsHistory,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recent stress alerts
            StressAlertList(alerts = uiState.stressAlerts)

            Spacer(modifier = Modifier.height(16.dp))

            // Medications being taken
            MedicationList(medications = uiState.medications)
        }
    }
}