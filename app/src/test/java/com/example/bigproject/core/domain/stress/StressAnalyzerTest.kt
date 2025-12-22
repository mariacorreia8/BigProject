package com.example.bigproject.core.domain.stress

import com.example.bigproject.core.domain.model.VitalReading
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StressAnalyzerTest {

    private val config = StressThresholdConfig(
        thresholdHigh = 70,
        minDurationMs = 1_000,
        movingAverageWindow = 3,
        cooldownMs = 5_000
    )

    @Test
    fun `should emit alert when stress stays above threshold`() = runTest {
        val analyzer = StressAnalyzer(config)
        val patientId = "patient-1"
        val patientName = "Carlos"

        var alert = analyzer.evaluate(highStress(0), patientId, patientName)
        assertNull(alert)

        alert = analyzer.evaluate(highStress(500), patientId, patientName)
        assertNull(alert)

        alert = analyzer.evaluate(highStress(1_500), patientId, patientName)
        assertNotNull(alert)
    }

    @Test
    fun `should not emit alert when stress stays below threshold`() = runTest {
        val analyzer = StressAnalyzer(config)
        val patientId = "patient-1"
        val patientName = "Carlos"

        var alert = analyzer.evaluate(lowStress(0), patientId, patientName)
        assertNull(alert)
        alert = analyzer.evaluate(lowStress(1_000), patientId, patientName)
        assertNull(alert)
    }

    private fun highStress(timestamp: Long) = VitalReading(
        heartRate = 90,
        spo2 = 98,
        stressLevel = 85,
        timestamp = timestamp,
        id = "",
        patientId = "patient-1",
        bodyBattery = 0,
        deviceSource = ""
    )

    private fun lowStress(timestamp: Long) = VitalReading(
        heartRate = 70,
        spo2 = 98,
        stressLevel = 40,
        timestamp = timestamp,
        id = "",
        patientId = "patient-1",
        bodyBattery = 0,
        deviceSource = ""
    )
}
