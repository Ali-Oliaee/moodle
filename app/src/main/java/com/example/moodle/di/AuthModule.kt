package com.example.moodle.di

import android.content.Context
import com.example.moodle.data.api.ApiClient
import com.example.moodle.data.api.ApiService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Auth-related dependencies
 * Provides singleton instances of API services and repositories
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    
    /**
     * Provides UserPreferences singleton
     * Handles user-specific data storage
     */
    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences {
        return UserPreferences(context)
    }
    
    /**
     * Provides ApiService singleton
     * Handles network API calls
     */
    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return ApiClient.createApiService()
    }
    
    /**
     * Provides AuthRepository singleton
     * Handles authentication business logic
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        userPreferences: UserPreferences,
        apiService: ApiService
    ): AuthRepository {
        return AuthRepository(
            prefs = userPreferences,
            apiService = apiService
        )
    }
}

