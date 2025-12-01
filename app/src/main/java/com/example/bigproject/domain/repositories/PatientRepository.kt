package com.example.bigproject.domain.repositories

import com.example.bigproject.models.AppUser

interface PatientRepository {
    suspend fun getPatientByToken(token: String): AppUser?
}