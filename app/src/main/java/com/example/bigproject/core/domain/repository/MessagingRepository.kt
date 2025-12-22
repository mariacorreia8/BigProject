package com.example.bigproject.core.domain.repository

interface MessagingRepository {
    suspend fun registerToken(token: String)
}

