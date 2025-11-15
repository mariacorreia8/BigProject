// DashboardContent.kt
package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bigproject.models.VitalReading
import com.teuprojeto.ui.dashboard.TrendChart
import com.teuprojeto.ui.dashboard.VitalsSummaryCard

@Composable
fun DashboardContent(modifier: Modifier = Modifier) {
    val sampleReadings = listOf(
        VitalReading(
            id = "1",
            patientId = "p1",
            timestamp = System.currentTimeMillis() - 60000L * 3,
            heartRate = 72,
            spo2 = 97,
            stressLevel = 30,
            bodyBattery = 80,
            deviceSource = "Garmin"
        ),
        VitalReading(
            id = "2",
            patientId = "p1",
            timestamp = System.currentTimeMillis() - 60000L * 2,
            heartRate = 78,
            spo2 = 96,
            stressLevel = 40,
            bodyBattery = 75,
            deviceSource = "Garmin"
        ),
        VitalReading(
            id = "3",
            patientId = "p1",
            timestamp = System.currentTimeMillis() - 60000L,
            heartRate = 80,
            spo2 = 98,
            stressLevel = 50,
            bodyBattery = 70,
            deviceSource = "Garmin"
        )
    )

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        VitalsSummaryCard(vitals = sampleReadings.lastOrNull())

        TrendChart(
            readings = sampleReadings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}