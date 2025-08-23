package com.example.moodle.data.network

import com.example.moodle.data.network.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @FormUrlEncoded
    @POST("login/token.php")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("service") service: String
    ): Response<LoginResponse>
}
