package com.example.bigproject.core.di

import android.content.Context
import com.example.bigproject.R
import com.example.bigproject.core.data.repositories.AuthRepositoryImpl
import com.example.bigproject.core.domain.repository.NurseHomeRepository
import com.example.bigproject.core.data.repositories.NurseHomeRepositoryImpl
import com.example.bigproject.core.domain.repository.AuthRepository
import com.example.bigproject.core.domain.stress.StressThresholdConfig
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository = authRepositoryImpl

    @Provides
    @Singleton
    fun provideNurseHomeRepository(impl: NurseHomeRepositoryImpl): NurseHomeRepository = impl

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }

    @Provides
    @Singleton
    fun provideApiBaseUrl(@ApplicationContext context: Context): String =
        context.getString(R.string.api_base_url)

    @Provides
    @Singleton
    fun provideStressThresholdConfig(): StressThresholdConfig = StressThresholdConfig()
}
