// MainActivity.kt
package com.example.bigproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bigproject.ui.auth.*
import com.example.bigproject.ui.dashboard.DashboardContent
import com.example.bigproject.ui.dashboard.NurseHomeScreen
import com.example.bigproject.ui.theme.BigProjectTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o Firebase (seguro)
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        setContent {
            BigProjectTheme {
                val navController = rememberNavController()
                val authViewModel = remember { AuthViewModel() }

                // Observa o estado de login
                val loginState by authViewModel.loginState.collectAsState()
                val userData by authViewModel.userData.collectAsState()

                // Se já está logado, tenta buscar dados do user
                LaunchedEffect(authViewModel.isUserLoggedIn()) {
                    if (authViewModel.isUserLoggedIn()) {
                        authViewModel.fetchUserData()
                    }
                }

                // Determina a tela inicial
                val startDestination = when {
                    !authViewModel.isUserLoggedIn() -> "auth_entry"
                    userData?.role == "Nurse" -> "nurse_home"
                    userData?.role == "Patient" -> "patient_dashboard"
                    else -> "auth_entry" // fallback
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    // === TELA INICIAL (Bem-vindo!) ===
                    composable("auth_entry") {
                        AuthEntryScreen(
                            onLoginClick = { navController.navigate("login") },
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }

                    // === TELA DE LOGIN ===
                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {
                                // Após login, verifica role
                                when (userData?.role) {
                                    "Nurse" -> navController.navigate("nurse_home") {
                                        popUpTo("auth_entry") { inclusive = true }
                                    }
                                    "Patient" -> navController.navigate("patient_dashboard") {
                                        popUpTo("auth_entry") { inclusive = true }
                                    }
                                    else -> navController.navigate("auth_entry")
                                }
                            }
                        )
                    }

                    // === TELA DE REGISTO ===
                    composable("register") {
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onRegisterSuccess = {
                                when (userData?.role) {
                                    "Nurse" -> navController.navigate("nurse_home") {
                                        popUpTo("auth_entry") { inclusive = true }
                                    }
                                    "Patient" -> navController.navigate("patient_dashboard") {
                                        popUpTo("auth_entry") { inclusive = true }
                                    }
                                    else -> navController.navigate("auth_entry")
                                }
                            },
                            onBackToLogin = { navController.navigateUp() }
                        )
                    }

                    // === DASHBOARD DO ENFERMEIRO ===
                    composable("nurse_home") {
                        NurseHomeScreen(
                            nurseName = userData?.name ?: "Enfermeiro(a)",
                            onScanQrClick = {
                                // Por agora, vai direto ao dashboard do paciente
                                // Depois: abre câmera para scan QR
                                navController.navigate("patient_dashboard")
                            }
                        )
                    }

                    // === DASHBOARD DO PACIENTE (com dados de teste) ===
                    composable("patient_dashboard") {
                        val sampleReadings = remember {
                            listOf(
                                com.example.bigproject.models.VitalReading(
                                    id = "1",
                                    patientId = "p1",
                                    timestamp = System.currentTimeMillis() - 60000L * 3,
                                    heartRate = 72,
                                    spo2 = 97,
                                    stressLevel = 30,
                                    bodyBattery = 80,
                                    deviceSource = "Garmin"
                                ),
                                com.example.bigproject.models.VitalReading(
                                    id = "2",
                                    patientId = "p1",
                                    timestamp = System.currentTimeMillis() - 60000L * 2,
                                    heartRate = 78,
                                    spo2 = 96,
                                    stressLevel = 40,
                                    bodyBattery = 75,
                                    deviceSource = "Garmin"
                                ),
                                com.example.bigproject.models.VitalReading(
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
                        }

                        DashboardContent(readings = sampleReadings)
                    }
                }
            }
        }
    }
}