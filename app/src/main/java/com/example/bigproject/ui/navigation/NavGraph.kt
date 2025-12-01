package com.example.bigproject.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bigproject.models.UserRole
import com.example.bigproject.models.VitalReading
import com.example.bigproject.ui.auth.AuthEntryScreen
import com.example.bigproject.ui.auth.AuthViewModel
import com.example.bigproject.ui.auth.LoginScreen
import com.example.bigproject.ui.auth.RegisterScreen
import com.example.bigproject.ui.dashboard.DigitalTwinScreen
import com.example.bigproject.ui.dashboard.NurseHomeScreen
import com.example.bigproject.ui.dashboard.PatientDashboardScreen
import com.example.bigproject.ui.dashboard.PillScanScreen
import com.example.bigproject.ui.dashboard.ScanPatientScreen
import com.example.bigproject.ui.dashboard.SettingsScreen
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

@Composable
fun NavGraph(client: HttpClient) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val userData by authViewModel.userData.collectAsState()

    val startDestination = when {
        !authViewModel.isUserLoggedIn() -> "auth_entry"
        userData?.role == UserRole.Nurse -> "nurse/home"
        userData?.role == UserRole.Patient -> "patient/home/${userData?.name}/true"
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
                        UserRole.Nurse -> "nurse/home"
                        UserRole.Patient -> "patient/home/${userData?.name}/true"
                        else -> "auth_entry"
                    }
                ) { popUpTo("auth_entry") { inclusive = true } }
            })
        }

        composable("register") {
            RegisterScreen(authViewModel = authViewModel, onRegisterSuccess = {
                navController.navigate(
                    when (userData?.role) {
                        UserRole.Nurse -> "nurse/home"
                        UserRole.Patient -> "patient/home/${userData?.name}/true"
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
                navArgument("patientName") { type = NavType.StringType },
                navArgument("hasData") { type = NavType.BoolType }
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
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate("auth_entry") { popUpTo(0) { inclusive = true } }
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