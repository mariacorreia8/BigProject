package com.example.bigproject.data.healthconnect

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import com.example.bigproject.data.model.VitalReading

/**
 * Mapper helpers to convert Health Connect SDK records to our domain model.
 */
object HealthConnectMappers {
    fun heartRateSamplesToReadings(
        record: HeartRateRecord,
        source: String
    ): List<VitalReading> {
        return record.samples.map { sample ->
            VitalReading(
                id = "",
                patientId = "",
                timestamp = sample.time.toEpochMilli(),
                heartRate = sample.beatsPerMinute.toInt(),
                heartRateVariability = null,
                spo2 = null,
                deviceSource = source
            )
        }
    }

    fun spo2RecordToReading(
        record: OxygenSaturationRecord,
        source: String
    ): VitalReading {
        return VitalReading(
            id = "",
            patientId = "",
            timestamp = record.time.toEpochMilli(),
            heartRate = null,
            heartRateVariability = null,
            spo2 = record.percentage.value,
            deviceSource = source
        )
    }

    fun hrvRecordToReading(
        record: HeartRateVariabilityRmssdRecord,
        source: String
    ): VitalReading {
        return VitalReading(
            id = "",
            patientId = "",
            timestamp = record.time.toEpochMilli(),
            heartRate = null,
            heartRateVariability = record.heartRateVariabilityMillis,
            spo2 = null,
            deviceSource = source
        )
    }
}