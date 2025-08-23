package com.example.moodle.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.moodle.data.network.ApiService
import com.example.moodle.data.network.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Response

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
        
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
    
    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(email, password, "moodle_mobile_app")
            
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.success && loginResponse.token != null) {
                    // Save authentication data
                    saveAuthData(loginResponse)
                    Result.success(loginResponse)
                } else {
                    Result.failure(Exception(loginResponse.message))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logout user
     */
    suspend fun logout() {
        clearAuthData()
    }
    
    /**
     * Check if user is logged in
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }
    
    /**
     * Get current user ID
     */
    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ID]
    }
    
    /**
     * Get current user email
     */
    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }
    
    /**
     * Get current user name
     */
    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME]
    }
    
    /**
     * Get current user role
     */
    val userRole: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ROLE]
    }
    
    /**
     * Get authentication token
     */
    suspend fun getAuthToken(): String? {
        return context.dataStore.data.first()[KEY_AUTH_TOKEN]
    }
    
    /**
     * Get refresh token
     */
    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[KEY_REFRESH_TOKEN]
    }
    
    /**
     * Save authentication data after successful login
     */
    private suspend fun saveAuthData(loginResponse: LoginResponse) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = loginResponse.token!!
            preferences[KEY_REFRESH_TOKEN] = loginResponse.refreshToken!!
            preferences[KEY_IS_LOGGED_IN] = true
            
            loginResponse.data?.let { userData ->
                preferences[KEY_USER_ID] = userData.id
                preferences[KEY_USER_EMAIL] = userData.username
                preferences[KEY_USER_NAME] = userData.name
                preferences[KEY_USER_ROLE] = userData.role
            }
        }
    }
    
    /**
     * Update tokens after refresh
     */
    private suspend fun updateTokens(newToken: String, newRefreshToken: String?) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = newToken
            newRefreshToken?.let { preferences[KEY_REFRESH_TOKEN] = it }
        }
    }
    
    /**
     * Clear all authentication data
     */
    private suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
