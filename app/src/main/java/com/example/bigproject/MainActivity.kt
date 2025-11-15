// MainActivity.kt
package com.example.bigproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bigproject.ui.auth.*
import com.example.bigproject.ui.dashboard.*
import com.example.bigproject.ui.theme.BigProjectTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        setContent {
            BigProjectTheme {
                val navController = rememberNavController()
                val authViewModel = remember { AuthViewModel() }

                // Verifica se já está logado
                val startDestination = if (authViewModel.isUserLoggedIn()) {
                    "dashboard"
                } else {
                    "auth_entry"
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    composable("auth_entry") {
                        AuthEntryScreen(
                            onLoginClick = { navController.navigate("login") },
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }

                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("auth_entry") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onRegisterSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("auth_entry") { inclusive = true }
                                }
                            },
                            onBackToLogin = { navController.navigateUp() }
                        )
                    }

                    composable("dashboard") {
                        DashboardContent()
                    }
                }
            }
        }
    }
}
