package com.example.moodle.data.model

data class LoginResponse(
    val success: Boolean,
    val token: String?,   
    val message: String?
)
