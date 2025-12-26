package com.example.bigproject.core.domain.repository

import com.example.bigproject.core.domain.model.user.AppUser

interface AuthRepository {
    suspend fun getCurrentUser(): AppUser?
    fun saveUser(user: AppUser)
    fun saveToken(token: String)
    fun getToken(): String?
    fun saveMessagingToken(token: String)
    fun getMessagingToken(): String?
    suspend fun createSessionToken(): Result<String>
    fun clear()
    
    // Firebase Auth methods
    suspend fun registerWithEmailAndPassword(email: String, password: String, name: String, role: String): Result<AppUser>
    suspend fun loginWithEmailAndPassword(email: String, password: String): Result<AppUser>
    suspend fun getCurrentFirebaseUser(): AppUser?
    fun signOut()
}
