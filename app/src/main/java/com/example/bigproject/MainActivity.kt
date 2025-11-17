package com.example.bigproject

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bigproject.ui.auth.*
import com.example.bigproject.ui.dashboard.DigitalTwinScreen
import com.example.bigproject.ui.dashboard.NurseHomeScreen
import com.example.bigproject.ui.dashboard.PatientDashboardScreen
import com.example.bigproject.ui.dashboard.PillScanScreen
import com.example.bigproject.ui.dashboard.ScanPatientScreen
import com.example.bigproject.ui.dashboard.SettingsScreen
import com.example.bigproject.ui.theme.BigProjectTheme
import com.example.bigproject.models.VitalReading
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Define the Ktor client here to be reused
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        // ---- UI ----
        setContent {

            BigProjectTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val userData by authViewModel.userData.collectAsState()

                val startDestination = when {
                    !authViewModel.isUserLoggedIn() -> "auth_entry"
                    userData?.role == "Nurse" -> "nurse/home"
                    userData?.role == "Patient" -> "patient/home"
                    else -> "auth_entry"
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    composable("auth_entry") {
                        AuthEntryScreen(
                            onLoginClick = { navController.navigate("login") },
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }

                    composable("login") {
                        LoginScreen(authViewModel = authViewModel, onLoginSuccess = {
                            navController.navigate(
                                when (userData?.role) {
                                    "Nurse" -> "nurse/home"
                                    "Patient" -> "patient/home"
                                    else -> "auth_entry"
                                }
                            ) { popUpTo("auth_entry") { inclusive = true } }
                        })
                    }

                    composable("register") {
                        RegisterScreen(authViewModel = authViewModel, onRegisterSuccess = {
                            navController.navigate(
                                when (userData?.role) {
                                    "Nurse" -> "nurse/home"
                                    "Patient" -> "patient/home"
                                    else -> "auth_entry"
                                }
                            ) { popUpTo("auth_entry") { inclusive = true } }
                        }, onBackToLogin = { navController.navigateUp() })
                    }

                    composable("nurse/home") {
                        NurseHomeScreen(
                            nurseName = userData?.name ?: "Enfermeiro(a)",
                            authViewModel = authViewModel,
                            onScanQrClick = { navController.navigate("scan_patient") },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate("auth_entry") { popUpTo(0) { inclusive = true } }
                            }
                        )
                    }

                    composable("scan_patient") {
                        ScanPatientScreen(
                            client = client, // Pass the client instance here
                            authViewModel = authViewModel,
                            onPatientFound = { name, email, reading ->
                                navController.navigate("patient/home/${Uri.encode(email)}/${reading != null}")
                            },
                            onPatientNotFound = {
                                navController.navigate("patient/home/unknown/true")
                            }
                        )
                    }

                    composable(
                        route = "patient/home/{patientName}/{hasData}",
                        arguments = listOf(
                            navArgument("patientName") { type = androidx.navigation.NavType.StringType },
                            navArgument("hasData") { type = androidx.navigation.NavType.BoolType }
                        )
                    ) { backStackEntry ->
                        val encodedName = backStackEntry.arguments?.getString("patientName") ?: "Paciente"
                        val patientName = Uri.decode(encodedName)
                        val hasData = backStackEntry.arguments?.getBoolean("hasData") ?: false

                        var latestReading by remember { mutableStateOf<VitalReading?>(null) }
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(patientName, hasData) {
                            if (hasData && patientName != "Desconhecido") {
                                scope.launch {
                                    val reading = authViewModel.getLatestVitalReading(patientName)
                                    latestReading = reading
                                }
                            }
                        }

                        PatientDashboardScreen(
                            patientName = patientName,
                            latestReading = latestReading,
                            noDataMessage = if (!hasData || latestReading == null) "Sem dados introduzidos" else null,
                            onExitClick = {
                                navController.navigate("nurse/home") {
                                    popUpTo("patient/home") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("pillscan") {
                        PillScanScreen()
                    }

                    composable("digitalTwin") {
                        DigitalTwinScreen()
                    }

                    composable("settings") {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}
