package com.example.bigproject.core.domain.stress

import com.example.bigproject.core.domain.model.StressAlert
import com.example.bigproject.core.domain.model.VitalReading
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stateful analyzer that raises a StressAlert when stress values remain above the configured
 * threshold for a sustained duration. A simple moving average smooths noisy readings.
 */
data class StressThresholdConfig(
    val thresholdHigh: Int = 70,
    val minDurationMs: Long = 2 * 60_000, // 2 minutes above threshold before alerting
    val movingAverageWindow: Int = 3,
    val cooldownMs: Long = 5 * 60_000 // avoid spamming alerts
)

class StressAnalyzer @Inject constructor(
    private val config: StressThresholdConfig
) {

    private val recentStress = ArrayDeque<Pair<Long, Int>>()
    private var aboveThresholdSince: Long? = null
    private var lastAlertTimestamp: Long = 0L

    fun evaluate(reading: VitalReading, patientId: String, patientName: String): StressAlert? {
        if (patientId.isBlank()) return null
        val timestamp = reading.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
        recentStress.addLast(timestamp to reading.stressLevel)
        while (recentStress.size > config.movingAverageWindow) {
            recentStress.removeFirst()
        }

        val averageStress = recentStress.map { it.second }.average().toInt()

        if (averageStress >= config.thresholdHigh) {
            if (aboveThresholdSince == null) {
                aboveThresholdSince = recentStress.firstOrNull()?.first ?: timestamp
            }
            val sustainedMs = timestamp - (aboveThresholdSince ?: timestamp)
            val cooldownElapsed = timestamp - lastAlertTimestamp >= config.cooldownMs
            if (sustainedMs >= config.minDurationMs && cooldownElapsed) {
                lastAlertTimestamp = timestamp
                aboveThresholdSince = null
                val severity = averageStress.coerceIn(0, 100)
                return StressAlert(
                    id = UUID.randomUUID().toString(),
                    patientId = patientId,
                    timestamp = timestamp,
                    severity = severity,
                    message = "$patientName está com stress elevado ($severity)",
                    acknowledged = false
                )
            }
        } else {
            aboveThresholdSince = null
        }
        return null
    }
}
