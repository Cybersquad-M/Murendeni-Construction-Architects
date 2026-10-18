package com.cybersquad.murendeni.api

// These classes match the JSON that the website API sends and receives.
// The property names must match the API's JSON names exactly.

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phone: String?,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: Int,
    val fullName: String,
    val email: String,
    val role: String   // "Client", "Designer" or "Sales"
)