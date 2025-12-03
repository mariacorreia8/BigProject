package com.example.bigproject.domain.entities

sealed interface AppUser {
    val uid: String
    val name: String
    val email: String
    val role: UserRole
}