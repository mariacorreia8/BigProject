package com.example.bigproject.data.healthconnect
/*
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HealthConnectSyncScheduler {

    private const val UNIQUE_WORK_NAME = "HealthConnectPeriodicSync"

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // precisa de net
            .setRequiresBatteryNotLow(true)                // opcional
            .build()

        val periodicRequest =
            PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(
                15, TimeUnit.MINUTES                      // ⏱ de 15 em 15 minutos
            )
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }
}
*/