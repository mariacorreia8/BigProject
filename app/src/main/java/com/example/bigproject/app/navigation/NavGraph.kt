package com.example.bigproject.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bigproject.core.domain.model.user.UserRole
import com.example.bigproject.feature.auth.ui.AuthEntryScreen
import com.example.bigproject.feature.auth.ui.AuthViewModel
import com.example.bigproject.feature.auth.ui.LoginScreen
import com.example.bigproject.feature.auth.ui.RegisterScreen
import com.example.bigproject.feature.dashboard.ui.patient.DigitalTwinScreen
import com.example.bigproject.feature.dashboard.ui.nurse.NurseHomeScreen
import com.example.bigproject.feature.dashboard.ui.nurse.NursePatientDashboard
import com.example.bigproject.feature.dashboard.ui.patient.PatientHomeScreen
import com.example.bigproject.feature.pillscanner.ui.PillScanScreen
import com.example.bigproject.feature.dashboard.ui.settings.SettingsScreen
import com.example.bigproject.feature.qr.ui.ShowQrCodeScreen
import com.example.bigproject.feature.qr.ui.ScanQrScreen
import com.example.bigproject.feature.dashboard.ui.patient.BreathingExerciseScreen

@Composable
fun NavGraph(notificationPatientId: String? = null) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val userData by authViewModel.userData.collectAsState()

    val startDestination = when {
        !authViewModel.isUserLoggedIn() -> "auth_entry"
        userData?.role == UserRole.Nurse -> if (notificationPatientId != null) "nurse/patientDashboard/${notificationPatientId}" else "nurse/home"
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
                NursePatientDashboard(patientId = patientId)
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

        composable("patient/breathing") {
            BreathingExerciseScreen(navController = navController)
        }
    }
}