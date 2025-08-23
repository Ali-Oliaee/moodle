package com.example.moodle.di

import android.content.Context
import com.example.moodle.data.network.ApiService
import com.example.moodle.data.network.NetworkConfig
import com.example.moodle.data.repository.AuthRepository
import com.example.moodle.presentation.viewmodels.LoginViewModel

object NetworkModule {
    
    /**
     * Create API service instance
     */
    fun provideApiService(): ApiService {
        return NetworkConfig.retrofit.create(ApiService::class.java)
    }
    
    /**
     * Create auth repository
     */
    fun provideAuthRepository(context: Context): AuthRepository {
        return AuthRepository(provideApiService(), context)
    }
    
    /**
     * Create login view model
     */
    fun provideLoginViewModel(context: Context): LoginViewModel {
        return LoginViewModel(provideAuthRepository(context))
    }
}
