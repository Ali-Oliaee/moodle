package com.example.moodle.di

import android.content.Context
import com.example.moodle.data.api.ApiClient
import com.example.moodle.data.api.ApiService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.repository.AuthRepository

/**
 * Simple dependency injection container
 * Manages creation and lifecycle of app dependencies
 * 
 * This is a manual DI approach that avoids external libraries and ensures
 * compatibility with support libraries (pre-AndroidX)
 */
class DependencyContainer(private val applicationContext: Context) {
    
    // ==================== Singletons ====================
    
    /**
     * UserPreferences singleton
     * Lazy initialization ensures it's only created when first accessed
     */
    val userPreferences: UserPreferences by lazy {
        UserPreferences(applicationContext)
    }
    
    /**
     * ApiService singleton
     * Lazy initialization ensures it's only created when first accessed
     */
    val apiService: ApiService by lazy {
        ApiClient.createApiService()
    }
    
    /**
     * AuthRepository singleton
     * Depends on: userPreferences, apiService
     */
    val authRepository: AuthRepository by lazy {
        AuthRepository(
            prefs = userPreferences,
            apiService = apiService
        )
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Additional dependencies can be added here as needed
     * Use lazy { } for singletons
     * Use regular functions for factory-created instances
     */
}

