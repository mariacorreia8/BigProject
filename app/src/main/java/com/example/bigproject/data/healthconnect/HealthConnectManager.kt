package com.example.bigproject.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord


class HealthConnectManager(
    private val context: Context
) {

    val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }
    val readPermissions= setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
    )

    // ====== PERMISSÕES PARA HEART RATE ======
    // Use Permission objects when requesting permissions from the UI
    val heartRatePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    suspend fun hasHeartRatePermission(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(heartRatePermissions)
    }

    companion object {
        // Verifica se o Health Connect está disponível no dispositivo
        fun getAvailabilityStatus(context: Context): Int {
            return HealthConnectClient.getSdkStatus(
                context,
                "com.google.android.apps.healthdata"
            )
        }

        // Contract para pedir permissões na UI (ActivityResult)
        fun createPermissionsContract() =
            PermissionController.createRequestPermissionResultContract()
    }
}
