package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.AuthRepository
import com.example.bigproject.models.AppUser

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AppUser? {
        return null
    }
}
