package com.example.bigproject.domain.entities

data class Patient(
    override val uid: String,
    override val name: String,
    override val email: String,
    override val role: UserRole = UserRole.Patient
) : AppUser