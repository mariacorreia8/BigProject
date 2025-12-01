package com.example.bigproject.domain.repositories

import com.example.bigproject.domain.entities.AppUser

interface AuthRepository {
    suspend fun getCurrentUser(): AppUser?
    fun saveUser(user: AppUser)
    fun saveToken(token: String)
    fun getToken(): String?
    fun clear()
}
