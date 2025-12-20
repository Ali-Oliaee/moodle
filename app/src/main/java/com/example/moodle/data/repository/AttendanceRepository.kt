package com.example.moodle.data.repository

import com.example.moodle.data.api.ApiClient
import com.example.moodle.data.api.ApiService
import com.example.moodle.data.model.GetParticipantsRequest
import com.example.moodle.data.model.ParticipantResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AttendanceRepository {
    
    private val apiService: ApiService = ApiClient.createApiService()
    
    fun getParticipants(
        accessToken: String,
        page: Int = 0,
        limit: Int = 100,
        onSuccess: (List<com.example.moodle.data.model.Participant>) -> Unit,
        onError: (String) -> Unit
    ) {
        val token = if (accessToken.startsWith("Bearer ")) accessToken
        else "Bearer $accessToken"

        
        val request = GetParticipantsRequest(page = page, limit = limit)
        
        apiService.getParticipants(token, request).enqueue(object : Callback<ParticipantResponse> {
            override fun onResponse(
                call: Call<ParticipantResponse>,
                response: Response<ParticipantResponse>
            ) {
                if (response.isSuccessful) {
                    val participantResponse = response.body()
                    if (participantResponse != null) {
                        onSuccess(participantResponse.items)
                    } else {
                        onError("خطا در دریافت اطلاعات")
                    }
                } else {
                    onError("خطای سرور: ${response.code()}")
                }
            }
            
            override fun onFailure(call: Call<ParticipantResponse>, t: Throwable) {
                onError("خطا در اتصال: ${t.message}")
            }
        })
    }
}

