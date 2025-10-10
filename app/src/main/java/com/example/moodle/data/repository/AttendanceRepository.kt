package com.example.moodle.data.repository

import android.util.Log
import com.example.moodle.data.api.ApiClient
import com.example.moodle.data.api.ApiService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.model.AttendanceRecordRequest
import com.example.moodle.data.model.AttendanceSubmitRequest
import com.example.moodle.data.model.AttendanceSubmitResponse
import com.example.moodle.data.model.StudentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AttendanceRepository(
    private val prefs: UserPreferences,
    private val apiService: ApiService = ApiClient.createApiService()
) {

    companion object {
        private const val TAG = "AttendanceRepository"
    }

    suspend fun fetchStudents(): Result<List<StudentDto>> = withContext(Dispatchers.IO) {
        val token = prefs.getAuthToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Authentication token not found. Please login again."))
        }

        try {
            val response = apiService.getStudents("Bearer $token").execute()
            if (response.isSuccessful) {
                val students = response.body()
                if (students != null) {
                    Result.success(students)
                } else {
                    Result.failure(IllegalStateException("Empty response received from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val message = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "fetchStudents failed: $message, body=$errorBody")
                Result.failure(IllegalStateException(message))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "fetchStudents exception: ${t.message}", t)
            Result.failure(mapNetworkException(t))
        }
    }

    suspend fun submitAttendance(records: List<AttendanceRecordRequest>): Result<AttendanceSubmitResponse> = withContext(Dispatchers.IO) {
        val token = prefs.getAuthToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Authentication token not found. Please login again."))
        }

        try {
            val response = apiService.submitAttendance("Bearer $token", AttendanceSubmitRequest(records)).execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Empty response received from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val message = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "submitAttendance failed: $message, body=$errorBody")
                Result.failure(IllegalStateException(message))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "submitAttendance exception: ${t.message}", t)
            Result.failure(mapNetworkException(t))
        }
    }

    private fun mapNetworkException(t: Throwable): Exception {
        return when (t) {
            is java.net.UnknownHostException -> Exception("Cannot connect to server. Check your internet connection.")
            is java.net.ConnectException -> Exception("Connection failed. Please check the server URL.")
            is java.net.SocketTimeoutException -> Exception("Connection timeout. Please try again.")
            else -> Exception(t.message ?: "Network error")
        }
    }
}
