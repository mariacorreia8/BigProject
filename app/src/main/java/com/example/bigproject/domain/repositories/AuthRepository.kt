package com.example.bigproject.domain.repositories

import com.example.bigproject.models.User

interface AuthRepository {
    suspend fun login(email: String, password: String): User?
    fun getCurrentUser(): User?
}