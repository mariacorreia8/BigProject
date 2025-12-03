package com.example.bigproject.data.healthconnect
/*
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectSyncController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val MANUAL_SYNC_WORK_NAME = "HealthConnectManualSync"
    }

    /**
     * Enfileira uma sincronização manual imediata do Health Connect.
     * Usa ExistingWorkPolicy.KEEP para evitar duplicar se já houver uma em execução.
     */
    fun triggerManualSync() {
        val syncRequest = OneTimeWorkRequestBuilder<HealthConnectSyncWorker>()
            .build()

        workManager.enqueueUniqueWork(
            MANUAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Observa o estado da sincronização manual.
     * Retorna um Flow com o estado atual do trabalho.
     */
    fun observeSyncState(): Flow<SyncState> {
        return workManager.getWorkInfosForUniqueWorkFlow(MANUAL_SYNC_WORK_NAME)
            .map { workInfos ->
                val workInfo = workInfos.firstOrNull()
                when {
                    workInfo == null -> SyncState.Idle
                    workInfo.state == WorkInfo.State.RUNNING -> SyncState.Syncing
                    workInfo.state == WorkInfo.State.SUCCEEDED -> SyncState.Success
                    workInfo.state == WorkInfo.State.FAILED -> SyncState.Error
                    else -> SyncState.Idle
                }
            }
    }
}

enum class SyncState {
    Idle,
    Syncing,
    Success,
    Error
}

*/