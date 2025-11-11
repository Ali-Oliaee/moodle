package com.example.moodle.data.model

data class LoginResponse(
    val success: Boolean,
    val accessToken: String?,   
    val message: String?
)
