package com.example.bigproject.ui.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bigproject.data.VitalsRepository
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

    fun fetchLatestVitals() {
        viewModelScope.launch {
            val since = Instant.now().minus(1, ChronoUnit.DAYS)
            _vitals.value = vitalsRepository.getLatestVitals(since)
        }
    }
}
