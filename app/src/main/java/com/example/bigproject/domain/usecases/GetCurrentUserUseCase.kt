package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.AuthRepository
import com.example.bigproject.models.AppUser

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppUser? = authRepository.getCurrentUser()
}
