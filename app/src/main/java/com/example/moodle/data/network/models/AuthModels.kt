package com.example.moodle.data.network.models

import com.google.gson.annotations.SerializedName

// Login Request
data class LoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("service")
    val service: String
)

// Login Response
data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: LoginData?,
    @SerializedName("token")
    val token: String?,
    @SerializedName("refreshToken")
    val refreshToken: String?
)

data class LoginData(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("role")
    val role: String
)
