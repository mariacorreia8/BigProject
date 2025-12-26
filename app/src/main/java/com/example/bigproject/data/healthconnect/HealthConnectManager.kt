package com.example.bigproject.data.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.bigproject.data.model.VitalReading
import com.example.bigproject.data.healthconnect.HealthConnectMappers

import java.time.Instant


class HealthConnectManager(
    private val context: Context
) {
    companion object {
        // Package da app oficial Health Connect
        private const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }

    val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    private val requiredPermissionsInternal = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    )
    private val optionalPermissionsInternal = setOf(
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    )
    val permissions: Set<String> = requiredPermissionsInternal + optionalPermissionsInternal
    val requiredPermissions: Set<String> = requiredPermissionsInternal

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    fun buildPermissionsIntent(): Intent {
        return Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS").apply {
            data = Uri.parse("package:$PROVIDER_PACKAGE_NAME")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    // Verifica se o Health Connect está disponível e o estado das permissões.

    suspend fun getAvailability(): HealthConnectAvailability {
        val sdkStatus = HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME)

        return when (sdkStatus) {
            // SDK_AVAILABLE => Health Connect instalado
            HealthConnectClient.SDK_AVAILABLE -> {
                if (hasRequiredPermissions()) {
                    HealthConnectAvailability.InstalledAndAvailable
                } else {
                    HealthConnectAvailability.PermissionsNotGranted
                }
            }

            // Não tem provider (por ex. Android < 9 / sem Play Services compatível, etc.)
            HealthConnectClient.SDK_UNAVAILABLE -> {
                HealthConnectAvailability.NotSupported
            }

            // Dá para instalar / atualizar a app Health Connect
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                HealthConnectAvailability.NotInstalled
            }

            else -> HealthConnectAvailability.NotSupported
        }
    }

    suspend fun hasRequiredPermissions(): Boolean {
        val granted: Set<String> = client
            .permissionController
            .getGrantedPermissions()

        val missing = requiredPermissionsInternal.filterNot { granted.contains(it) }
        if (missing.isNotEmpty()) {
            Log.d("HealthConnectManager", "Missing Health Connect permissions: $missing")
        }
        return missing.isEmpty()
    }

    suspend fun readLatestVitals(since: Instant): List<VitalReading> {
        // Heart Rate
        val hrResponse = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since)
            )
        )
        val hrRecords = hrResponse.records

        // SpO2
        val spo2Response = client.readRecords(
            ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since)
            )
        )
        val spo2Records = spo2Response.records

        // HRV
        val hrvResponse = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since)
            )
        )
        val hrvRecords = hrvResponse.records

        return combineRecordsToVitalReadings(
            heartRateRecords = hrRecords,
            spo2Records = spo2Records,
            hrvRecords = hrvRecords
        )
    }
    private fun combineRecordsToVitalReadings(
        heartRateRecords: List<HeartRateRecord>,
        spo2Records: List<OxygenSaturationRecord>,
        hrvRecords: List<HeartRateVariabilityRmssdRecord>
    ): List<VitalReading> {
        val readingsMap = mutableMapOf<Long, VitalReading>()
        val source = "HealthConnect" // default source label; replace if needed per record

        // Map heart rate samples
        heartRateRecords.forEach { hrRecord ->
            val hrReadings = HealthConnectMappers.heartRateSamplesToReadings(hrRecord, source)
            hrReadings.forEach { reading ->
                val tsSec = reading.timestamp / 1000
                val existing = readingsMap[tsSec]
                if (existing != null) {
                    val newSource = if (existing.deviceSource == "Unknown") reading.deviceSource else existing.deviceSource
                    readingsMap[tsSec] = existing.copy(heartRate = reading.heartRate, deviceSource = newSource)
                } else {
                    readingsMap[tsSec] = reading
                }
            }
        }

        // Map SpO2
        spo2Records.forEach { spo2Record ->
            val reading = HealthConnectMappers.spo2RecordToReading(spo2Record, source)
            val tsSec = reading.timestamp / 1000
            val existing = readingsMap[tsSec]
            if (existing != null) {
                val newSource = if (existing.deviceSource == "Unknown") reading.deviceSource else existing.deviceSource
                readingsMap[tsSec] = existing.copy(spo2 = reading.spo2, deviceSource = newSource)
            } else {
                readingsMap[tsSec] = reading
            }
        }

        // Map HRV
        hrvRecords.forEach { hrvRecord ->
            val reading = HealthConnectMappers.hrvRecordToReading(hrvRecord, source)
            val tsSec = reading.timestamp / 1000
            val existing = readingsMap[tsSec]
            if (existing != null) {
                val newSource = if (existing.deviceSource == "Unknown") reading.deviceSource else existing.deviceSource
                readingsMap[tsSec] = existing.copy(heartRateVariability = reading.heartRateVariability, deviceSource = newSource)
            } else {
                readingsMap[tsSec] = reading
            }
        }

        return readingsMap.values.sortedByDescending { it.timestamp }
    }
}
