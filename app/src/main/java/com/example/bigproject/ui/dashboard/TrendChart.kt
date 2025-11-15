package com.teuprojeto.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bigproject.models.VitalReading

@Composable
fun TrendChart(readings: List<VitalReading>, modifier: Modifier = Modifier) {
    if (readings.isEmpty()) return

    val maxHR = readings.maxOf { it.heartRate ?: 0 }.coerceAtLeast(100)
    val minHR = readings.minOf { it.heartRate ?: 0 }.coerceAtMost(60)

    Canvas(modifier = modifier.height(200.dp).fillMaxWidth()) {
        val stepX = size.width / (readings.size - 1)
        val scaleY = size.height / (maxHR - minHR).toFloat()

        val points = readings.mapIndexed { index, reading ->
            val x = index * stepX
            val y = size.height - ((reading.heartRate ?: 0 - minHR) * scaleY)
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color.Red,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f
            )
        }
    }
}
