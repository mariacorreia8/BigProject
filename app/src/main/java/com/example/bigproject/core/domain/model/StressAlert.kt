package com.example.bigproject.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StressAlert(
    val id: String,
    val patientId: String,
    val timestamp: Long,
    val severity: Int,
    val message: String,
    val acknowledged: Boolean
)
