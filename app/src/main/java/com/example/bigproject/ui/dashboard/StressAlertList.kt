package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bigproject.domain.entities.StressAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StressAlertList(alerts: List<StressAlert>) {
    LazyColumn {
        items(alerts) { alert ->
            StressAlertItem(alert = alert)
        }
    }
}

@Composable
fun StressAlertItem(alert: StressAlert) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Stress Alert", style = MaterialTheme.typography.titleMedium)
            Text(text = "Severity: ${alert.severity}", style = MaterialTheme.typography.bodyMedium)
            Text(text = alert.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(alert.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
