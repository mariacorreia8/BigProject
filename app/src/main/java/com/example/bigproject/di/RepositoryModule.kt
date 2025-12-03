package com.example.bigproject.di

import com.example.bigproject.data.repositories.AlertRepositoryImpl
import com.example.bigproject.data.repositories.PatientRepositoryImpl
import com.example.bigproject.domain.repositories.AlertRepository
import com.example.bigproject.domain.repositories.PatientRepository
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
}
