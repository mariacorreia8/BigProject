// ui/dashboard/TrendChart.kt
package com.example.bigproject.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bigproject.domain.entities.VitalReading

@Composable
fun TrendChart(readings: List<VitalReading>, modifier: Modifier = Modifier) {
    if (readings.isEmpty()) {
        Text("Sem dados disponíveis")
        return
    }

    val maxHR = readings.maxOf { it.heartRate }.coerceAtLeast(100)
    val minHR = readings.minOf { it.heartRate }.coerceAtMost(60)
    
    val maxStress = readings.maxOf { it.stressLevel }.coerceAtLeast(100)
    val minStress = readings.minOf { it.stressLevel }.coerceAtMost(0)

    Column(modifier = modifier) {
        Text("Tendência de Batimentos Cardíacos e Stress", modifier = Modifier.padding(bottom = 8.dp))

        Canvas(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            val stepX = size.width / (readings.size - 1).toFloat()
            val scaleHR = size.height / (maxHR - minHR).toFloat()
            val scaleStress = size.height / (maxStress - minStress).toFloat()

            val hrPoints = readings.mapIndexed { index, reading ->
                val x = index * stepX
                val y = size.height - ((reading.heartRate - minHR) * scaleHR)
                Offset(x, y)
            }
            
            val stressPoints = readings.mapIndexed { index, reading ->
                val x = index * stepX
                val y = size.height - ((reading.stressLevel - minStress) * scaleStress)
                Offset(x, y)
            }

            for (i in 0 until hrPoints.size - 1) {
                drawLine(
                    color = Color.Red,
                    start = hrPoints[i],
                    end = hrPoints[i + 1],
                    strokeWidth = 4f
                )
            }
            
            for (i in 0 until stressPoints.size - 1) {
                drawLine(
                    color = Color.Blue,
                    start = stressPoints[i],
                    end = stressPoints[i + 1],
                    strokeWidth = 4f
                )
            }
        }
    }
}