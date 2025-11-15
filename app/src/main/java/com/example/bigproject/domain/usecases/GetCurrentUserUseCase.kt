package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.AuthRepository
import com.example.bigproject.models.User

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): User? = authRepository.getCurrentUser()
}