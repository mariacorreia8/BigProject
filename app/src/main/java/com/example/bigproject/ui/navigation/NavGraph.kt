package com.example.bigproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bigproject.domain.entities.UserRole
import com.example.bigproject.ui.auth.AuthEntryScreen
import com.example.bigproject.ui.auth.AuthViewModel
import com.example.bigproject.ui.auth.LoginScreen
import com.example.bigproject.ui.auth.RegisterScreen
import com.example.bigproject.ui.dashboard.DigitalTwinScreen
import com.example.bigproject.ui.dashboard.NurseHomeScreen
import com.example.bigproject.ui.dashboard.PatientHomeScreen
import com.example.bigproject.ui.dashboard.PillScanScreen
import com.example.bigproject.ui.dashboard.ScanPatientScreen
import com.example.bigproject.ui.dashboard.SettingsScreen
import io.ktor.client.HttpClient

@Composable
fun NavGraph(client: HttpClient) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val userData by authViewModel.userData.collectAsState()

    val startDestination = when {
        !authViewModel.isUserLoggedIn() -> "auth_entry"
        userData?.role == UserRole.Nurse -> "nurse/home"
        userData?.role == UserRole.Patient -> "patient/home"
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
                val destination = when (authViewModel.userData.value?.role) {
                    UserRole.Nurse -> "nurse/home"
                    UserRole.Patient -> "patient/home"
                    else -> "auth_entry"
                }
                navController.navigate(destination) { popUpTo("auth_entry") { inclusive = true } }
            })
        }

        composable("register") {
            RegisterScreen(authViewModel = authViewModel, onRegisterSuccess = {
                 val destination = when (authViewModel.userData.value?.role) {
                    UserRole.Nurse -> "nurse/home"
                    UserRole.Patient -> "patient/home"
                    else -> "auth_entry"
                }
                navController.navigate(destination) { popUpTo("auth_entry") { inclusive = true } }
            }, onBackToLogin = { navController.navigateUp() })
        }

        composable("nurse/home") {
            NurseHomeScreen(
                nurseName = userData?.name ?: "Enfermeiro(a)",
                onScanQrClick = { navController.navigate("scan_patient") },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate("auth_entry") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable("scan_patient") {
            ScanPatientScreen(
                client = client,
                authViewModel = authViewModel,
                onPatientFound = { name, email, vital ->
                    navController.navigate("patient/home")
                },
                onPatientNotFound = { /*TODO Decide what to do here, e.g., show a toast */ }
            )
        }

        composable("patient/home") {
            PatientHomeScreen()
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