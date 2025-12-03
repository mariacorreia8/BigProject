package com.example.bigproject.data.healthconnect

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.bigproject.models.VitalReading
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
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        // Adicionar outras permissões conforme necessário
    )

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
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
            .getGrantedPermissions() // API nova: sem argumentos, devolve Set<String>

        return granted.containsAll(permissions)
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

        // Process Heart Rate records
        for (hrRecord in heartRateRecords) {
            val source = hrRecord.metadata.device?.model ?: "Unknown"
            for (sample in hrRecord.samples) {
                val timestampSeconds = sample.time.epochSecond
                val existingReading = readingsMap[timestampSeconds]
                if (existingReading != null) {
                    val newSource = if (existingReading.deviceSource == "Unknown") source else existingReading.deviceSource
                    readingsMap[timestampSeconds] = existingReading.copy(
                        heartRate = sample.beatsPerMinute.toInt(),
                        deviceSource = newSource
                    )
                } else {
                    readingsMap[timestampSeconds] = VitalReading(
                        timestamp = sample.time.toEpochMilli(),
                        heartRate = sample.beatsPerMinute.toInt(),
                        heartRateVariability = null,
                        spo2 = null,
                        deviceSource = source
                    )
                }
            }
        }

        // Process SpO2 records
        for (spo2Record in spo2Records) {
            val timestampSeconds = spo2Record.time.epochSecond
            val source = spo2Record.metadata.device?.model ?: "Unknown"
            val existingReading = readingsMap[timestampSeconds]
            if (existingReading != null) {
                val newSource = if(existingReading.deviceSource == "Unknown") source else existingReading.deviceSource
                readingsMap[timestampSeconds] = existingReading.copy(spo2 = spo2Record.percentage.value, deviceSource = newSource)
            } else {
                readingsMap[timestampSeconds] = VitalReading(
                    timestamp = spo2Record.time.toEpochMilli(),
                    heartRate = null,
                    heartRateVariability = null,
                    spo2 = spo2Record.percentage.value,
                    deviceSource = source
                )
            }
        }

        // Process HRV records
        for (hrvRecord in hrvRecords) {
            val timestampSeconds = hrvRecord.time.epochSecond
            val source = hrvRecord.metadata.device?.model ?: "Unknown"
            val existingReading = readingsMap[timestampSeconds]
            if (existingReading != null) {
                val newSource = if(existingReading.deviceSource == "Unknown") source else existingReading.deviceSource
                readingsMap[timestampSeconds] = existingReading.copy(heartRateVariability = hrvRecord.heartRateVariabilityMillis, deviceSource = newSource)
            } else {
                readingsMap[timestampSeconds] = VitalReading(
                    timestamp = hrvRecord.time.toEpochMilli(),
                    heartRate = null,
                    heartRateVariability = hrvRecord.heartRateVariabilityMillis,
                    spo2 = null,
                    deviceSource = source
                )
            }
        }

        return readingsMap.values.sortedByDescending { it.timestamp }
    }
}
