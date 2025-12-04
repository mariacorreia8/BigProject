package com.example.bigproject.feature.dashboard.ui.patient

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingExerciseScreen(navController: NavController) {
    val phases = listOf("Inspira", "Segura", "Expira")
    val phaseDurations = listOf(4000L, 2000L, 4000L)
    val currentPhase = remember { mutableStateOf(phases.first()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            while (true) {
                phases.forEachIndexed { index, phase ->
                    currentPhase.value = phase
                    delay(phaseDurations[index])
                }
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "breathing")
    val progress by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercício de Respiração") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Siga o ritmo para reduzir o stress",
                style = MaterialTheme.typography.bodyLarge
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.height(240.dp)) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    style = Stroke(width = 8f)
                )
                drawCircle(
                    color = primaryColor,
                    radius = size.minDimension / 2 * progress,
                    style = Stroke(width = 12f)
                )
            }

            Text(
                text = currentPhase.value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF0EA5E9)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { navController.navigateUp() }) {
                Text("Terminar")
            }
        }
    }
}

