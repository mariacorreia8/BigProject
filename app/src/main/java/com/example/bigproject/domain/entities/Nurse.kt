package com.example.bigproject.domain.entities

data class Nurse(
    override val uid: String,
    override val name: String,
    override val email: String,
    override val role: UserRole = UserRole.Nurse
) : AppUser