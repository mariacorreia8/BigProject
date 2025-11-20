package com.example.bigproject.ui.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(@ApplicationContext context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secret_shared_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        with(sharedPreferences.edit()) {
            putString("auth_token", token)
            apply()
        }
    }

    fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun saveUser(name: String, role: String) {
        with(sharedPreferences.edit()) {
            putString("user_name", name)
            putString("user_role", role)
            apply()
        }
    }

    fun getUser(): AuthViewModel.UserData? {
        val name = sharedPreferences.getString("user_name", null)
        val role = sharedPreferences.getString("user_role", null)
        return if (name != null && role != null) {
            AuthViewModel.UserData(name, role)
        } else {
            null
        }
    }

    fun clear() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
}