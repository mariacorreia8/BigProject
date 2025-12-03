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
import com.example.bigproject.domain.entities.Medication

@Composable
fun MedicationList(medications: List<Medication>) {
    LazyColumn {
        items(medications) { medication ->
            MedicationItem(medication = medication)
        }
    }
}

@Composable
fun MedicationItem(medication: Medication) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = medication.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Dosage: ${medication.dosage}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Form: ${medication.form}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
