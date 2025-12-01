package com.example.bigproject.data.repositories

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.bigproject.domain.entities.AppUser
import com.example.bigproject.domain.entities.Nurse
import com.example.bigproject.domain.entities.Patient
import com.example.bigproject.domain.entities.UserRole
import com.example.bigproject.domain.repositories.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    override fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    override fun saveUser(user: AppUser) {
        sharedPreferences.edit()
            .putString("user_uid", user.uid)
            .putString("user_name", user.name)
            .putString("user_email", user.email)
            .putString("user_role", user.role.name)
            .apply()
    }

    override suspend fun getCurrentUser(): AppUser? {
        val uid = sharedPreferences.getString("user_uid", null) ?: return null
        val name = sharedPreferences.getString("user_name", "") ?: ""
        val email = sharedPreferences.getString("user_email", "") ?: ""
        val roleName = sharedPreferences.getString("user_role", null)

        return when (roleName) {
            UserRole.Patient.name -> Patient(uid, name, email)
            UserRole.Nurse.name -> Nurse(uid, name, email)
            else -> null
        }
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
