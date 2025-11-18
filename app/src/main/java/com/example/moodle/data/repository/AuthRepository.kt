package com.example.moodle.data.repository

import com.example.moodle.data.api.ApiService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.model.LoginRequest
import com.example.moodle.data.model.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModelProvider

/**
 * Repository handling authentication operations
 * Dependencies are injected via constructor (managed by Koin)
 *
 * @param prefs UserPreferences for storing auth tokens and user data
 * @param apiService ApiService for making network requests
 */
class AuthRepository(
    private val prefs: UserPreferences,
    private val apiService: ApiService
) {
    
    suspend fun login(
        username: String, 
        password: String, 
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(username, password)
            val response = apiService.login(request).execute()
        
            if (response.isSuccessful) {
                val body = response.body()
                
                when {
                    body == null -> {
                        Result.failure(Exception("Empty response from server"))
                    }
                    !body.accessToken.isNullOrBlank() -> {
                        prefs.saveAuthToken(body.accessToken)
                        prefs.saveUsername(username)
                        
                        Result.success(body)
                    }
                    else -> {
                        val errorMsg = body.message ?: "Login failed - No authentication token received"
                        Result.failure(Exception(errorMsg))
                    }
                }
            } else {
                // val errorBody = response.errorBody()?.string()
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (t: Throwable) {
            val errorMsg = when (t) {
                is java.net.UnknownHostException -> "Cannot connect to server. Check your internet connection and server URL."
                is java.net.ConnectException -> "Connection failed. Please check the server URL."
                is java.net.SocketTimeoutException -> "Connection timeout. Please try again."
                else -> "Network error: ${t.message ?: "Unknown error"}"
            }
            Result.failure(Exception(errorMsg))
        }
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.isLoggedIn()
    }
    
    fun logout() {
        prefs.clearAllData()
    }
    
    fun getAuthToken(): String? {
        return prefs.getAuthToken()
    }
    
    fun getUsername(): String? {
        return prefs.getUsername()
    }
}
