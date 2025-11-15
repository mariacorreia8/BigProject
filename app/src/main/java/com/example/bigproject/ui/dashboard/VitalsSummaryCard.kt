package com.teuprojeto.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bigproject.models.VitalReading

@Composable
fun VitalsSummaryCard(vitals: VitalReading?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Heart Rate
            VitalsItem(
                label = "HR",
                value = vitals?.heartRate?.toString() ?: "--",
                color = when (vitals?.heartRate ?: 0) {
                    in 60..100 -> Color.Green
                    else -> Color.Red
                }
            )

            // Stress Level
            VitalsItem(
                label = "Stress",
                value = vitals?.stressLevel?.toString() ?: "--",
                color = when (vitals?.stressLevel ?: 0) {
                    in 0..40 -> Color.Green
                    in 41..70 -> Color.Yellow
                    else -> Color.Red
                }
            )
        }
    }
}

@Composable
fun VitalsItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
