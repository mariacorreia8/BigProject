package com.example.bigproject

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bigproject.ui.auth.*
import com.example.bigproject.ui.dashboard.NurseHomeScreen
import com.example.bigproject.ui.dashboard.PatientDashboardScreen
import com.example.bigproject.ui.dashboard.ScanPatientScreen
import com.example.bigproject.ui.theme.BigProjectTheme
import com.example.bigproject.models.VitalReading
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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

class MainActivity : ComponentActivity() {

    // CLIENTE KTOR
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

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // ---- UI ----
        setContent {

            BigProjectTheme {
                val navController = rememberNavController()
                val authViewModel = remember { AuthViewModel(client) }
                val userData by authViewModel.userData.collectAsState()

                val startDestination = when {
                    !authViewModel.isUserLoggedIn() -> "auth_entry"
                    userData?.role == "Nurse" -> "nurse_home"
                    userData?.role == "Patient" -> "patient_dashboard"
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
                                    "Nurse" -> "nurse_home"
                                    "Patient" -> "patient_dashboard"
                                    else -> "auth_entry"
                                }
                            ) { popUpTo("auth_entry") { inclusive = true } }
                        })
                    }

                    composable("register") {
                        RegisterScreen(authViewModel = authViewModel, onRegisterSuccess = {
                            navController.navigate(
                                when (userData?.role) {
                                    "Nurse" -> "nurse_home"
                                    "Patient" -> "patient_dashboard"
                                    else -> "auth_entry"
                                }
                            ) { popUpTo("auth_entry") { inclusive = true } }
                        }, onBackToLogin = { navController.navigateUp() })
                    }

                    composable("nurse_home") {
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
                            authViewModel = authViewModel,
                            client = client,
                            onPatientFound = { name, email, reading ->
                                // PASSA EMAIL, não name!
                                navController.navigate("patient_dashboard/${Uri.encode(email)}/${reading != null}")
                            },
                            onPatientNotFound = {
                                navController.navigate("patient_dashboard/unknown/true")
                            }
                        )
                    }

                    // Route for logged-in patients (no parameters)
                    composable("patient_dashboard") {
                        val currentUser = auth.currentUser
                        val patientEmail = currentUser?.email ?: ""
                        val patientName = userData?.name ?: "Paciente"
                        
                        var latestReading by remember { mutableStateOf<VitalReading?>(null) }
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(patientEmail) {
                            if (patientEmail.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        val token = authViewModel.getIdToken() ?: return@launch
                                        val response: ApiPatientResponse = client.post("https://us-central1-bigproject-4a536.cloudfunctions.net/api/patients/search") {
                                            headers { append("Authorization", "Bearer $token") }
                                            contentType(ContentType.Application.Json)
                                            setBody(mapOf("email" to patientEmail))
                                        }.body()
                                        latestReading = response.latestVital?.toVitalReading()
                                    } catch (e: Exception) {
                                        latestReading = null
                                    }
                                }
                            }
                        }

                        PatientDashboardScreen(
                            patientName = patientName,
                            latestReading = latestReading,
                            noDataMessage = if (latestReading == null) "Sem dados introduzidos" else null,
                            onExitClick = {
                                authViewModel.logout()
                                navController.navigate("auth_entry") { popUpTo(0) { inclusive = true } }
                            }
                        )
                    }

                    // Route for nurses viewing a patient's dashboard (with parameters)
                    composable(
                        route = "patient_dashboard/{patientName}/{hasData}",
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
                                    try {
                                        val token = authViewModel.getIdToken() ?: return@launch // CORRIGIDO!
                                        val response: ApiPatientResponse = client.post("https://us-central1-bigproject-4a536.cloudfunctions.net/api/patients/search") {
                                            headers { append("Authorization", "Bearer $token") }
                                            contentType(ContentType.Application.Json)
                                            setBody(mapOf("email" to patientName))
                                        }.body()
                                        latestReading = response.latestVital?.toVitalReading()
                                    } catch (e: Exception) {
                                        latestReading = null
                                    }
                                }
                            }
                        }

                        PatientDashboardScreen(
                            patientName = patientName,
                            latestReading = latestReading,
                            noDataMessage = if (!hasData || latestReading == null) "Sem dados introduzidos" else null,
                            onExitClick = {
                                navController.navigate("nurse_home") {
                                    popUpTo("patient_dashboard") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        client.close()
        super.onDestroy()
    }

}

// === MODELOS DO KTOR ===
@Serializable
data class ApiPatientResponse(
    val found: Boolean,
    val patient: PatientInfo? = null,
    val latestVital: VitalData? = null
)

@Serializable
data class PatientInfo(val id: String, val name: String, val email: String)

@Serializable
data class VitalData(
    val heartRate: Int,
    val spo2: Int,
    val stressLevel: Int,
    val bodyBattery: Int? = null,
    val deviceSource: String
)

fun VitalData.toVitalReading() = VitalReading(
    id = "",
    patientId = "",
    timestamp = System.currentTimeMillis(),
    heartRate = heartRate,
    spo2 = spo2,
    stressLevel = stressLevel,
    bodyBattery = bodyBattery ?: 0,
    deviceSource = deviceSource
)
