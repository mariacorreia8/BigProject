package com.example.bigproject.data.repositories

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.bigproject.domain.repositories.AuthRepository
import com.example.bigproject.models.AppUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(@ApplicationContext context: Context) : AuthRepository {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secret_shared_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveToken(token: String) {
        with(sharedPreferences.edit()) {
            putString("auth_token", token)
            apply()
        }
    }

    override fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    override fun saveUser(user: AppUser) {
        val userJson = Json.encodeToString(user)
        with(sharedPreferences.edit()) {
            putString("user_data", userJson)
            apply()
        }
    }

    override suspend fun getCurrentUser(): AppUser? {
        val userJson = sharedPreferences.getString("user_data", null)
        return if (userJson != null) {
            try {
                Json.decodeFromString<AppUser>(userJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override fun clear() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
}
