package com.example.bigproject.ui.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.data.VitalsRepository
import com.example.bigproject.data.healthconnect.HealthConnectAvailability
import com.example.bigproject.models.VitalReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val vitalsRepository: VitalsRepository
) : ViewModel() {

    private val _vitals = MutableStateFlow<List<VitalReading>>(emptyList())
    val vitals: StateFlow<List<VitalReading>> = _vitals

    private val _availability = MutableStateFlow<HealthConnectAvailability?>(null)
    val availability: StateFlow<HealthConnectAvailability?> = _availability

    fun fetchLatestVitals() {
        viewModelScope.launch {
            val availability = vitalsRepository.getHealthConnectAvailability()
            _availability.value = availability
            if (availability is HealthConnectAvailability.InstalledAndAvailable) {
                val since = Instant.now().minus(1, ChronoUnit.DAYS)
                _vitals.value = vitalsRepository.getLatestVitals(since)
            } else {
                _vitals.value = emptyList()
            }
        }
    }
}
