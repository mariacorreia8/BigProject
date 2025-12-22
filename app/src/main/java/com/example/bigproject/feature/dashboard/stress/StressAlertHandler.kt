package com.example.bigproject.feature.dashboard.stress

import com.example.bigproject.core.domain.model.StressAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class StressAlertHandler @Inject constructor(
    private val notifier: StressAlertNotifier
) {
    private val _latestAlert = MutableStateFlow<StressAlert?>(null)
    val latestAlert: StateFlow<StressAlert?> = _latestAlert.asStateFlow()

    fun handle(alert: StressAlert) {
        // Show a notification and expose the alert to collectors
        try {
            notifier.show(alert)
        } catch (_: Exception) {
            // Swallow notifier exceptions to avoid crashing callers; logging can be added if desired
        }
        _latestAlert.value = alert
    }

    fun clear() {
        _latestAlert.value = null
    }
}

