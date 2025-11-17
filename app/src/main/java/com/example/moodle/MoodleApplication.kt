package com.example.moodle

import android.app.Application
import com.example.moodle.di.DependencyContainer

/**
 * Application class for initializing app-wide components
 * Manages the dependency injection container
 */
class MoodleApplication : Application() {
    
    /**
     * Dependency injection container
     * Holds all app dependencies and manages their lifecycle
     */
    lateinit var dependencyContainer: DependencyContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependency container
        dependencyContainer = DependencyContainer(applicationContext)
    }
    
    companion object {
        /**
         * Get the DependencyContainer from any Context
         * Usage: (context.applicationContext as MoodleApplication).dependencyContainer
         */
        fun getDependencyContainer(application: Application): DependencyContainer {
            return (application as MoodleApplication).dependencyContainer
        }
    }
}

