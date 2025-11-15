// ui/dashboard/NurseHomeScreen.kt
package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NurseHomeScreen(
    nurseName: String,
    onScanQrClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bem-vindo(a), $nurseName!",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onScanQrClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Scan QR Code para ver Paciente", fontSize = 18.sp)
        }
    }
}