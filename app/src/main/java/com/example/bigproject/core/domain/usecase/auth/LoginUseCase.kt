package com.example.bigproject.core.domain.usecase.auth

import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AppUser? {
        return null
    }
}