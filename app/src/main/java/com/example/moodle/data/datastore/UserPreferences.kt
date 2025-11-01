package com.example.moodle.data.datastore

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_BACKGROUND_PATTERN = "background_pattern"
        private const val KEY_CURRENT_PAGE_ID = "current_page_id"
        private const val KEY_PAGE_IDS = "page_ids"
        private const val KEY_NEXT_PAGE_ID = "next_page_id"
        private const val KEY_SHARED_COLOR = "shared_color"
        private const val KEY_SHARED_STROKE_WIDTH = "shared_stroke_width"
        private const val KEY_SHARED_BACKGROUND_PATTERN = "shared_background_pattern"
        private const val KEY_SHARED_BACKGROUND_COLOR = "shared_background_color"
    }

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAuthToken(): String? {
        return sharedPrefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveAuthToken(token: String) {
        sharedPrefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun clearAuthToken() {
        sharedPrefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .apply()
    }

    fun getUsername(): String? {
        return sharedPrefs.getString(KEY_USERNAME, null)
    }

    fun saveUsername(username: String) {
        sharedPrefs.edit()
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }


    fun getBackgroundPattern(): Int {
        return sharedPrefs.getInt(KEY_BACKGROUND_PATTERN, 0) // Default to PLAIN (id = 0)
    }

    fun saveBackgroundPattern(patternId: Int) {
        sharedPrefs.edit()
            .putInt(KEY_BACKGROUND_PATTERN, patternId)
            .apply()
    }

    // Page management persistence
    fun getCurrentPageId(): Int {
        return sharedPrefs.getInt(KEY_CURRENT_PAGE_ID, 1)
    }
    
    fun saveCurrentPageId(pageId: Int) {
        sharedPrefs.edit()
            .putInt(KEY_CURRENT_PAGE_ID, pageId)
            .apply()
    }
    
    fun getPageIds(): Set<String> {
        return sharedPrefs.getStringSet(KEY_PAGE_IDS, setOf("1")) ?: setOf("1")
    }
    
    fun savePageIds(pageIds: Set<Int>) {
        val stringSet = pageIds.map { it.toString() }.toSet()
        sharedPrefs.edit()
            .putStringSet(KEY_PAGE_IDS, stringSet)
            .apply()
    }
    
    fun getNextPageId(): Int {
        return sharedPrefs.getInt(KEY_NEXT_PAGE_ID, 2)
    }
    
    fun saveNextPageId(nextPageId: Int) {
        sharedPrefs.edit()
            .putInt(KEY_NEXT_PAGE_ID, nextPageId)
            .apply()
    }
    
    fun getSharedColor(): Int {
        return sharedPrefs.getInt(KEY_SHARED_COLOR, android.graphics.Color.BLACK)
    }
    
    fun saveSharedColor(color: Int) {
        sharedPrefs.edit()
            .putInt(KEY_SHARED_COLOR, color)
            .apply()
    }
    
    fun getSharedStrokeWidth(): Float {
        return sharedPrefs.getFloat(KEY_SHARED_STROKE_WIDTH, 8f)
    }
    
    fun saveSharedStrokeWidth(strokeWidth: Float) {
        sharedPrefs.edit()
            .putFloat(KEY_SHARED_STROKE_WIDTH, strokeWidth)
            .apply()
    }
    
    fun getSharedBackgroundPattern(): Int {
        return sharedPrefs.getInt(KEY_SHARED_BACKGROUND_PATTERN, 0)
    }
    
    fun saveSharedBackgroundPattern(patternId: Int) {
        sharedPrefs.edit()
            .putInt(KEY_SHARED_BACKGROUND_PATTERN, patternId)
            .apply()
    }
    
    fun getSharedBackgroundColor(): Int {
        return sharedPrefs.getInt(KEY_SHARED_BACKGROUND_COLOR, 0)
    }
    
    fun saveSharedBackgroundColor(colorId: Int) {
        sharedPrefs.edit()
            .putInt(KEY_SHARED_BACKGROUND_COLOR, colorId)
            .apply()
    }

    fun clearAllData() {
        sharedPrefs.edit().clear().apply()
    }
}
