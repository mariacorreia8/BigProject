package com.example.bigproject.data.healthconnect
/*
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.data.VitalsRepository


import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.temporal.ChronoUnit

@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val vitalsRepository: VitalsRepository,
    private val authRepository: AuthRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("HealthConnectSyncWorker", "🚀 doWork começou")

        return try {
            val availability = vitalsRepository.getHealthConnectAvailability()
            Log.d("HealthConnectSyncWorker", "Availability = $availability")

            if (availability !is HealthConnectAvailability.InstalledAndAvailable) {
                Log.d("HealthConnectSyncWorker", "Health Connect não disponível, saio.")
                return Result.success()
            }

            val user = authRepository.getCurrentUser()
            if (user == null) {
                Log.d("HealthConnectSyncWorker", "Nenhum utilizador autenticado, saio.")
                return Result.success()
            }

            val patientId = user.id  // ou uid
            val since = Instant.now().minus(15, ChronoUnit.MINUTES)

            val readings = vitalsRepository.getLatestVitals(since)
            val enriched = readings.map {
                it.copy(
                    patientId = patientId,
                    deviceSource = it.deviceSource.ifEmpty { "HealthConnect" }
                )
            }

            vitalsRepository.upsertVitals(enriched)
            Log.d("HealthConnectSyncWorker", "Sincronizadas ${enriched.size} leituras.")
            Result.success()
        } catch (e: Exception) {
            Log.e("HealthConnectSyncWorker", "Erro ao sincronizar", e)
            Result.retry()
        }
    }

}*/
