// ui/dashboard/NurseHomeScreen.kt
package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NurseHomeScreen(
    nurseName: String,
    authViewModel: com.example.bigproject.ui.auth.AuthViewModel,
    onScanQrClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === TÍTULO + BOTÃO SCAN ===
        Column(
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
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DB6AC))
            ) {
                Text("Scan QR Code para ver Paciente", fontSize = 18.sp)
            }
        }

        // === BOTÃO LOGOUT ===
        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
        ) {
            Text("Logout", fontSize = 18.sp, color = Color.White)
        }
    }
}