package com.example.registerlogin.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val userName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val userName: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)

