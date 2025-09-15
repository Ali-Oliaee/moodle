package com.example.moodle.data.repository

import com.example.moodle.data.api.ApiService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.model.LoginResponse
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: ApiService,
    private val prefs: UserPreferences
) {
    suspend fun login(username: String, password: String, service: String): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.login(username, password, service).execute()
                Log.w("Moodle", "Login response: $resp")
                Log.w("Moodle", "Login response body: ${resp.body()}")
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.success == true && !body.token.isNullOrBlank()) {
                        prefs.saveAuthToken(body.token!!)
                        Result.success(body)
                    } else {
                        Result.failure(Exception(body?.message ?: "Login failed"))
                    }
                } else {
                    Log.w("Moodle", "Login response error: ${resp.errorBody()}")
                    Result.failure(Exception("HTTP ${resp.code()} ${resp.message()}"))
                }
            } catch (t: Throwable) {
                Log.w("Moodle", "Login response error: $t")
                Result.failure(t)
            }
        }
}
