package com.example.moodle

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for initializing app-wide components
 * Uses Hilt for dependency injection
 */
@HiltAndroidApp
class MoodleApplication : Application()

