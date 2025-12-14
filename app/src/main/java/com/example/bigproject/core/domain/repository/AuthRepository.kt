package com.example.bigproject.core.domain.repository

import com.example.bigproject.core.domain.model.user.AppUser

interface AuthRepository {
    suspend fun getCurrentUser(): AppUser?
    fun saveUser(user: AppUser)
    fun saveToken(token: String)
    fun getToken(): String?
    suspend fun createSessionToken(): Result<String>
    fun clear()
}
