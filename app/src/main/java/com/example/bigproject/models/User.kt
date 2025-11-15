package com.example.bigproject.models

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val email: String,
    val linkedGarminDeviceId: String? = null,
    val patientIds: List<String>? = null
)

enum class UserRole {
    Nurse,
    Patient
}