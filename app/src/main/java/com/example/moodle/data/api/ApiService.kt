package com.example.moodle.data.api

import com.example.moodle.data.model.LoginRequest
import com.example.moodle.data.model.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>
}
