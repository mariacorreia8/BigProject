package com.example.bigproject.domain.repositories

import com.example.bigproject.models.User

interface PatientRepository {
    suspend fun getPatientByToken(token: String): User?
}