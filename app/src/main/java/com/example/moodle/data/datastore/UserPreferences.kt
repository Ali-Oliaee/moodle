package com.example.moodle.data.datastore

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USERNAME = "username"
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

    fun clearAllData() {
        sharedPrefs.edit().clear().apply()
    }
}
