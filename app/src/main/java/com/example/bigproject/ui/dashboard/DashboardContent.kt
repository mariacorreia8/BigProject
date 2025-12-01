// ui/dashboard/DashboardContent.kt
package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bigproject.domain.entities.VitalReading

@Composable
fun DashboardContent(readings: List<VitalReading>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        VitalsSummaryCard(vitals = readings.lastOrNull())

        TrendChart(
            readings = readings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}