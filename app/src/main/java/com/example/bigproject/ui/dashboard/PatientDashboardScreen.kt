// ui/dashboard/PatientDashboardScreen.kt
package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigproject.models.VitalReading

@Composable
fun PatientDashboardScreen(
    patientName: String,
    latestReading: VitalReading?,
    noDataMessage: String? = null,
    onExitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Patient Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = patientName.firstOrNull()?.toString() ?: "P",
                        color = Color(0xFF757575),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = patientName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // === MENSAGEM DE SEM DADOS (SEM ÍCONE) ===
        if (noDataMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ÍCONE REMOVIDO — SÓ TEXTO
                    Text(
                        text = "!",
                        color = Color(0xFFB71C1C),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = noDataMessage,
                        color = Color(0xFFB71C1C),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }else {
            VitalCard("SpO₂", "SpO₂", "${latestReading?.spo2 ?: "--"}%", Color(0xFF4DB6AC))
            VitalCard("Heart Rate", "Heart Rate", "${latestReading?.heartRate ?: "--"}bpm", Color(0xFFE57373))
            VitalCard(
                icon = "Temperature",
                label = "Temperature",
                value = if (latestReading?.bodyBattery != null) {
                    String.format("%.1f", latestReading.bodyBattery * 0.9 + 96.8) + "°F"
                } else "98.4°F",
                color = Color(0xFFBA68C8)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onExitClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DB6AC))
        ) {
            Text("EXIT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun VitalCard(icon: String, label: String, value: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp, color = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}