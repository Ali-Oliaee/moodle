package com.example.moodle.di

import android.content.Context
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.whiteboard.managers.ExportManager
import com.example.moodle.ui.whiteboard.managers.ImportManager
import com.example.moodle.ui.whiteboard.managers.PageManager
import com.example.moodle.ui.whiteboard.managers.TextRecognitionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Whiteboard-related dependencies
 * Provides singleton instances of managers for whiteboard functionality
 */
@Module
@InstallIn(SingletonComponent::class)
object WhiteboardModule {
    
    /**
     * Provides PageManager singleton
     * Manages multiple pages in the whiteboard
     */
    @Provides
    @Singleton
    fun providePageManager(
        userPreferences: UserPreferences
    ): PageManager {
        return PageManager(userPreferences)
    }
    
    /**
     * Provides ExportManager singleton
     * Manages exporting whiteboard content to various formats
     */
    @Provides
    @Singleton
    fun provideExportManager(
        @ApplicationContext context: Context
    ): ExportManager {
        return ExportManager(context)
    }
    
    /**
     * Provides ImportManager singleton
     * Manages importing content into the whiteboard
     */
    @Provides
    @Singleton
    fun provideImportManager(
        @ApplicationContext context: Context
    ): ImportManager {
        return ImportManager(context)
    }
    
    /**
     * Provides TextRecognitionManager singleton
     * Manages text recognition functionality using ML Kit
     */
    @Provides
    @Singleton
    fun provideTextRecognitionManager(
        @ApplicationContext context: Context
    ): TextRecognitionManager {
        return TextRecognitionManager(context)
    }
}

