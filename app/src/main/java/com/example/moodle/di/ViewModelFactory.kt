package com.example.moodle.di

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.example.moodle.ui.login.LoginViewModel

/**
 * Factory for creating ViewModels with constructor dependencies
 * 
 * This factory uses the DependencyContainer to inject dependencies
 * into ViewModels, following the Factory pattern
 */
class ViewModelFactory(
    private val application: Application,
    private val dependencies: DependencyContainer
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(
                    application = application,
                    authRepository = dependencies.authRepository
                ) as T
            }
            // Add more ViewModels here as needed
            // modelClass.isAssignableFrom(OtherViewModel::class.java) -> { ... }
            
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

