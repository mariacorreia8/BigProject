package com.example.bigproject.domain.usecases

import com.example.bigproject.domain.repositories.AuthRepository
import com.example.bigproject.models.AppUser

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AppUser? {
        // The login logic is now handled in the AuthViewModel, so this use case is temporarily disabled.
        // To re-enable, you would move the Ktor API call from the ViewModel to here.
        return null
    }
}
