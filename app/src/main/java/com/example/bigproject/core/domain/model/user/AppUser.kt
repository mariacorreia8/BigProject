package com.example.bigproject.core.domain.model.user

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    Nurse,
    Patient
}

@Serializable
sealed interface AppUser {
    val id: String
    val name: String
    val email: String
    val role: UserRole
}

@Serializable
data class Patient(
    override val id: String = "",
    override val name: String = "",
    override val email: String = "",
    override val role: UserRole = UserRole.Patient,
    val usesHealthConnect: Boolean = false,
    val nurseIds: List<String> = emptyList()
) : AppUser

@Serializable
data class Nurse(
    override val id: String = "",
    override val name: String = "",
    override val email: String = "",
    override val role: UserRole = UserRole.Nurse,
    val patientIds: List<String> = emptyList()
) : AppUser
