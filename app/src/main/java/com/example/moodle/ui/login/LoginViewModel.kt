package com.example.moodle.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moodle.data.repository.AuthRepository
import com.example.moodle.data.datastore.UserPreferences
import kotlinx.coroutines.*

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository(UserPreferences(application))
    
    // UI State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn
    
    init {
        checkLoginStatus()
    }
    
    fun checkLoginStatus() {
        _isLoggedIn.value = authRepository.isLoggedIn()
    }
    
    fun login(username: String, password: String) {
        if (username.isBlank()) {
            _errorMessage.value = "Username cannot be empty"
            return
        }
        
        if (password.isBlank()) {
            _errorMessage.value = "Password cannot be empty"
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            try {
                val result = authRepository.login(username, password)
                
                result.fold(
                    onSuccess = { loginResponse ->
                        _isLoading.value = false
                        _loginSuccess.value = true
                        _isLoggedIn.value = true
                    },
                    onFailure = { exception ->
                        _isLoading.value = false
                        _errorMessage.value = exception.message ?: "Login failed"
                        _loginSuccess.value = false
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Unexpected error: ${e.message}"
                _loginSuccess.value = false
            }
        }
    }
    
    fun logout() {
        authRepository.logout()
        _isLoggedIn.value = false
        _loginSuccess.value = false
    }
    
    fun getAuthToken(): String? {
        return authRepository.getAuthToken()
    }
    
    fun getUsername(): String? {
        return authRepository.getUsername()
    }
    
    fun clearError() {
        _errorMessage.value = ""
    }
}
