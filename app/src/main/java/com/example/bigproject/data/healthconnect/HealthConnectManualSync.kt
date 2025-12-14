package com.example.bigproject.data.healthconnect

import android.content.Context
import android.util.Log
import com.example.bigproject.data.VitalsRepository
import com.example.bigproject.data.firebase.FirebaseVitalsRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Sincroniza os dados do Health Connect para o Firestore **uma vez**.
 * É chamada diretamente (por exemplo, a partir de um botão).
 */
suspend fun syncHealthConnectNow(
    context: Context,
    patientId: String
) {
    Log.d("HealthConnectManualSync", "syncHealthConnectNow invoked for patient=$patientId")
    // 1) Construir dependências à mão (sem Hilt, sem WorkManager)
    val appContext = context.applicationContext

    val manager = HealthConnectManager(appContext)
    val healthRepo = HealthConnectVitalsRepository(manager)

    val firestore = FirebaseFirestore.getInstance()
    val firebaseRepo = FirebaseVitalsRepository(firestore)

    val vitalsRepo = VitalsRepository(
        healthRepo = healthRepo,
        fbRepo = firebaseRepo
    )

    // 2) Verificar disponibilidade do Health Connect
    val availability = vitalsRepo.getHealthConnectAvailability()
    Log.d("HealthConnectManualSync", "HealthConnect availability=$availability")
    if (availability !is HealthConnectAvailability.InstalledAndAvailable) {
        Log.w("HealthConnectManualSync", "Health Connect not available; exiting")
        return
    }

    // 3) Janela temporal: últimos 15 minutos
    val since = Instant.now().minus(15, ChronoUnit.MINUTES)

    // 4) Usar o método que já tens no VitalsRepository para ler + guardar no Firebase
    try {
        vitalsRepo.syncLastestVitals(patientId, since)
        Log.d("HealthConnectManualSync", "syncHealthConnectNow completed successfully")
    } catch (e: Exception) {
        Log.e("HealthConnectManualSync", "syncHealthConnectNow failed", e)
        throw e
    }
}
