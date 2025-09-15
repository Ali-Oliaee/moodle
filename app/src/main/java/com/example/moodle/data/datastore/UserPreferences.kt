package com.example.moodle.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_AUTH_TOKEN: Preferences.Key<String> =
            stringPreferencesKey("auth_token")
    }

    val authTokenFlow: Flow<String?> =
        context.userDataStore.data.map { it[KEY_AUTH_TOKEN] }

    suspend fun saveAuthToken(token: String) {
        context.userDataStore.edit { prefs -> prefs[KEY_AUTH_TOKEN] = token }
    }

    suspend fun clearAuthToken() {
        context.userDataStore.edit { prefs -> prefs.remove(KEY_AUTH_TOKEN) }
    }
}
