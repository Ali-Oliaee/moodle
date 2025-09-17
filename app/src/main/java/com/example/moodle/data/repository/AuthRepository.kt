package com.example.moodle.data.repository

import com.example.moodle.data.api.ApiClient
import com.example.moodle.data.api.ApiService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.model.LoginResponse
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val prefs: UserPreferences
) {
    
    suspend fun login(
        username: String, 
        password: String, 
        service: String = "moodle_mobile_app"
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val apiService = ApiClient.createApiService()

            val response = apiService.login(username, password, service).execute()
        
            if (response.isSuccessful) {
                val body = response.body()
                
                when {
                    body == null -> {
                        Log.d("Moodle-Auth", "Login failed: Empty response body")
                        Result.failure(Exception("Empty response from server"))
                    }
                    !body.token.isNullOrBlank() -> {
                        prefs.saveAuthToken(body.token!!)
                        prefs.saveUsername(username)
                        
                        Result.success(body)
                    }
                    else -> {
                        Log.d("Moodle-Auth", "Login failed: No token received")
                        Log.d("Moodle-Auth", "Success flag: ${body.success}")
                        Log.d("Moodle-Auth", "Message: ${body.message}")
                        
                        val errorMsg = body.message ?: "Login failed - No authentication token received"
                        Result.failure(Exception(errorMsg))
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (t: Throwable) {
            Log.e("Moodle", "Login exception: ${t.message}", t)
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
