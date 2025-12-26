package com.example.bigproject.core.data.repositories

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.bigproject.core.domain.model.user.AppUser
import com.example.bigproject.core.domain.model.user.Patient
import com.example.bigproject.core.domain.model.user.Nurse
import com.example.bigproject.core.domain.model.user.UserRole
import com.example.bigproject.core.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SessionTokenResponse(val token: String)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val httpClient: HttpClient,
    private val apiBaseUrl: String,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
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
        // Return stored token (will be updated when user signs in)
        return sharedPreferences.getString("auth_token", null)
    }

    override fun saveMessagingToken(token: String) {
        sharedPreferences.edit().putString("messaging_token", token).apply()
    }

    override fun getMessagingToken(): String? {
        return sharedPreferences.getString("messaging_token", null)
    }

    override suspend fun createSessionToken(): Result<String> {
        return try {
            val authToken = getToken() ?: return Result.failure(Exception("User not authenticated"))
            val response = httpClient.post("$apiBaseUrl/patient-session/token") {
                bearerAuth(authToken)
            }
            if (response.status.value in 200..299) {
                val responseBody = response.bodyAsText()
                val sessionToken = Json.decodeFromString<SessionTokenResponse>(responseBody).token
                Result.success(sessionToken)
            } else {
                Result.failure(Exception("Failed to create session token: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun saveUser(user: AppUser) {
        sharedPreferences.edit()
            .putString("user_id", user.id)
            .putString("user_name", user.name)
            .putString("user_email", user.email)
            .putString("user_role", user.role.name)
            .apply()
    }

    override suspend fun getCurrentUser(): AppUser? {
        val id = sharedPreferences.getString("user_id", null) ?: return null
        val name = sharedPreferences.getString("user_name", "") ?: ""
        val email = sharedPreferences.getString("user_email", "") ?: ""
        val roleName = sharedPreferences.getString("user_role", null)

        return when (roleName) {
            UserRole.Patient.name -> Patient(id, name, email)
            UserRole.Nurse.name -> Nurse(id, name, email)
            else -> null
        }
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
        firebaseAuth.signOut()
    }
    
    override suspend fun registerWithEmailAndPassword(
        email: String,
        password: String,
        name: String,
        role: String
    ): Result<AppUser> = withContext(Dispatchers.IO) {
        try {
            // Create user in Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return@withContext Result.failure(Exception("Failed to create user"))
            
            // Update user profile with display name
            val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdate).await()
            
            // Get ID token
            val idToken = user.getIdToken(false).await().token
                ?: return@withContext Result.failure(Exception("Failed to get ID token"))
            
            // Create user document in Firestore
            val userData = HashMap<String, Any>().apply {
                put("id", user.uid)
                put("name", name)
                put("email", email)
                put("role", role)
                put("createdAt", com.google.firebase.Timestamp.now())
                
                when (role) {
                    "Nurse" -> put("patientIds", emptyList<String>())
                    "Patient" -> {
                        put("nurseIds", emptyList<String>())
                        put("usesHealthConnect", false)
                    }
                }
            }
            
            firestore.collection("users").document(user.uid)
                .set(userData, SetOptions.merge())
                .await()
            
            // Get the created user document
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val userDocData = userDoc.data ?: return@withContext Result.failure(Exception("Failed to retrieve user data"))
            
            // Save token and user locally
            saveToken(idToken)
            val appUser = when (role) {
                "Patient" -> Patient(
                    id = user.uid,
                    name = userDocData["name"] as? String ?: name,
                    email = userDocData["email"] as? String ?: email
                )
                "Nurse" -> Nurse(
                    id = user.uid,
                    name = userDocData["name"] as? String ?: name,
                    email = userDocData["email"] as? String ?: email
                )
                else -> return@withContext Result.failure(Exception("Invalid role"))
            }
            saveUser(appUser)
            
            Result.success(appUser)
        } catch (e: FirebaseAuthException) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loginWithEmailAndPassword(
        email: String,
        password: String
    ): Result<AppUser> = withContext(Dispatchers.IO) {
        try {
            // Sign in with Firebase Auth
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return@withContext Result.failure(Exception("Failed to sign in"))
            
            // Get ID token
            val idToken = user.getIdToken(false).await().token
                ?: return@withContext Result.failure(Exception("Failed to get ID token"))
            
            // Get user document from Firestore
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            
            val userDocData = if (userDoc.exists()) {
                userDoc.data!!
            } else {
                // User exists in Auth but not in Firestore - create a basic document
                val defaultUserData = HashMap<String, Any>().apply {
                    put("id", user.uid)
                    put("name", user.displayName ?: user.email?.split("@")?.get(0) ?: "User")
                    put("email", user.email ?: email)
                    put("role", "Patient")
                    put("createdAt", com.google.firebase.Timestamp.now())
                    put("nurseIds", emptyList<String>())
                    put("usesHealthConnect", false)
                }
                firestore.collection("users").document(user.uid)
                    .set(defaultUserData, SetOptions.merge())
                    .await()
                defaultUserData
            }
            
            // Save token and user locally
            saveToken(idToken)
            val role = userDocData["role"] as? String ?: "Patient"
            val appUser = when (role) {
                "Patient" -> Patient(
                    id = user.uid,
                    name = userDocData["name"] as? String ?: "",
                    email = userDocData["email"] as? String ?: email
                )
                "Nurse" -> Nurse(
                    id = user.uid,
                    name = userDocData["name"] as? String ?: "",
                    email = userDocData["email"] as? String ?: email
                )
                else -> return@withContext Result.failure(Exception("Invalid role in user document"))
            }
            saveUser(appUser)
            
            Result.success(appUser)
        } catch (e: FirebaseAuthException) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentFirebaseUser(): AppUser? = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = firebaseAuth.currentUser ?: return@withContext null
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            
            if (!userDoc.exists()) return@withContext null
            
            val userDocData = userDoc.data ?: return@withContext null
            val role = userDocData["role"] as? String ?: return@withContext null
            
            when (role) {
                "Patient" -> Patient(
                    id = firebaseUser.uid,
                    name = userDocData["name"] as? String ?: "",
                    email = userDocData["email"] as? String ?: ""
                )
                "Nurse" -> Nurse(
                    id = firebaseUser.uid,
                    name = userDocData["name"] as? String ?: "",
                    email = userDocData["email"] as? String ?: ""
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun signOut() {
        firebaseAuth.signOut()
        clear()
    }
    
    private fun getAuthErrorMessage(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address"
            "ERROR_WRONG_PASSWORD" -> "Wrong password"
            "ERROR_USER_NOT_FOUND" -> "User not found. Please register first."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak"
            "ERROR_INVALID_CREDENTIAL" -> "Invalid email or password"
            else -> e.message ?: "Authentication failed"
        }
    }
}
