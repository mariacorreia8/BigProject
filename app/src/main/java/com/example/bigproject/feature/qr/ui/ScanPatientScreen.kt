package com.example.bigproject.feature.qr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigproject.core.domain.model.VitalReading
import com.example.bigproject.feature.auth.ui.AuthViewModel
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

@Composable
fun ScanPatientScreen(
    authViewModel: AuthViewModel,
    client: HttpClient,
    onPatientFound: (String, String, VitalReading?) -> Unit,
    onPatientNotFound: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Introduza o email do paciente",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email do paciente") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isBlank()) return@Button
                    isLoading = true
                    error = null

                    scope.launch {
                        try {
                            val token = authViewModel.getIdToken() ?: throw Exception("Sem token")

                            val response: ApiPatientResponse = client.post("https://us-central1-bigproject-4a536.cloudfunctions.net/api/patients/search") {
                                headers { append("Authorization", "Bearer $token") }
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("email" to email.trim()))
                            }.body()

                            // LOGS DE DEBUG (remove depois)
                            println("API RESPONSE: $response")
                            println("latestVital: ${response.latestVital}")

                            if (response.found && response.patient != null) {
                                val reading = response.latestVital?.toVitalReading()
                                println("VitalReading: $reading")
                                onPatientFound(response.patient.name, response.patient.email, reading)
                            } else {
                                onPatientNotFound()
                            }
                        } catch (e: Exception) {
                            error = "Erro: ${e.message}"
                            e.printStackTrace()
                            onPatientNotFound()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Procurar Paciente", fontSize = 18.sp)
            }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

// === MODELOS PARA A API ===
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
    val id: String? = null,
    val timestamp: Long? = null,
    val heartRate: Int,
    val spo2: Int,
    val stressLevel: Int,
    val bodyBattery: Int? = null,
    val deviceSource: String
)

fun VitalData.toVitalReading() = VitalReading(
    id = id ?: "",
    patientId = "",
    timestamp = timestamp ?: System.currentTimeMillis(),
    heartRate = heartRate,
    spo2 = spo2,
    stressLevel = stressLevel,
    bodyBattery = bodyBattery ?: 0,
    deviceSource = deviceSource
)