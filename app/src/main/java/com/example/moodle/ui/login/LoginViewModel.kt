package com.example.moodle.ui.login

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.LiveData
import com.example.moodle.data.repository.AuthRepository
import com.example.moodle.data.datastore.UserPreferences
import kotlinx.coroutines.*

import com.example.moodle.R

data class LoginViewState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false,
    val successMessage: String? = null,
    val shouldNavigate: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository

    private val _uiState = MutableLiveData<LoginViewState>()
    val uiState: LiveData<LoginViewState> = _uiState

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        authRepository = AuthRepository(UserPreferences(application))
        _uiState.value = LoginViewState()
    }

    fun performLogin(username: String, password: String) {
        val context = getApplication<Application>()
        
        if (username.isBlank()) {
            updateState(error = context.getString(R.string.login_empty_username))
            return
        }
        if (password.isBlank()) {
            updateState(error = context.getString(R.string.login_empty_password))
            return
        }

        updateState(isLoading = true, error = null)
        
        scope.launch {
            try {
                val result = authRepository.login(username, password)
                
                result.fold(
                    onSuccess = { loginResponse ->
                        updateState(
                            isLoading = false,
                            isLoginSuccessful = true,
                            successMessage = context.getString(R.string.login_success),
                            shouldNavigate = true
                        )
                    },
                    onFailure = { exception ->
                        updateState(
                            isLoading = false,
                            error = exception.message ?: context.getString(R.string.login_error)
                        )
                    }
                )
            } catch (e: Exception) {
                updateState(
                    isLoading = false,
                    error = context.getString(R.string.login_error) + ": ${e.message}"
                )
            }
        }
    }

    private fun updateState(
        username: String? = null,
        password: String? = null,
        isLoading: Boolean? = null,
        error: String? = null,
        isLoginSuccessful: Boolean? = null,
        successMessage: String? = null,
        shouldNavigate: Boolean? = null
    ) {
        val currentState = _uiState.value ?: LoginViewState()
        _uiState.value = currentState.copy(
            username = username ?: currentState.username,
            password = password ?: currentState.password,
            isLoading = isLoading ?: currentState.isLoading,
            error = error,
            isLoginSuccessful = isLoginSuccessful ?: currentState.isLoginSuccessful,
            successMessage = successMessage,
            shouldNavigate = shouldNavigate ?: currentState.shouldNavigate
        )
    }

    fun getIsLoggedIn(): Boolean {
        return _uiState.value?.isLoginSuccessful ?: false
    }

    fun onNavigationHandled() {
        updateState(shouldNavigate = false)
    }

    fun cancelJob() {
        job.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}