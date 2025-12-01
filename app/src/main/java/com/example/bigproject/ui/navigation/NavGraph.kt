package com.example.bigproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bigproject.domain.entities.UserRole
import com.example.bigproject.ui.auth.AuthEntryScreen
import com.example.bigproject.ui.auth.AuthViewModel
import com.example.bigproject.ui.auth.LoginScreen
import com.example.bigproject.ui.auth.RegisterScreen
import com.example.bigproject.ui.dashboard.DigitalTwinScreen
import com.example.bigproject.ui.dashboard.NurseHomeScreen
import com.example.bigproject.ui.dashboard.PatientHomeScreen
import com.example.bigproject.ui.dashboard.PillScanScreen
import com.example.bigproject.ui.dashboard.SettingsScreen
import com.example.bigproject.ui.dashboard.ShowQrCodeScreen
import com.example.bigproject.ui.nurse.NursePatientDashboardScreen
import com.example.bigproject.ui.nurse.ScanQrScreen

@Composable
fun NavGraph() {
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
                onScanQrClick = { navController.navigate("nurse/scan_qr") },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate("auth_entry") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable("nurse/scan_qr") {
            ScanQrScreen(navController = navController)
        }

        composable(
            route = "nurse/patientDashboard/{patientId}",
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId")
            if (patientId != null) {
                NursePatientDashboardScreen(patientId = patientId)
            }
        }

        composable("patient/home") {
            PatientHomeScreen(
                navController = navController,
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate("auth_entry") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable("patient/show_qr_code") {
            ShowQrCodeScreen()
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