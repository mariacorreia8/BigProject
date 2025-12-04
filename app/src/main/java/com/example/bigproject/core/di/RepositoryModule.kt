package com.example.bigproject.core.di

import com.example.bigproject.core.data.repositories.AlertRepositoryImpl
import com.example.bigproject.core.data.repositories.MessagingRepositoryImpl
import com.example.bigproject.feature.dashboard.data.PatientRepositoryImpl
import com.example.bigproject.core.domain.repository.AlertRepository
import com.example.bigproject.core.domain.repository.MessagingRepository
import com.example.bigproject.core.domain.repository.PatientRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlertRepository(alertRepositoryImpl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindPatientRepository(patientRepositoryImpl: PatientRepositoryImpl): PatientRepository

    @Binds
    @Singleton
    abstract fun bindMessagingRepository(impl: MessagingRepositoryImpl): MessagingRepository
}
