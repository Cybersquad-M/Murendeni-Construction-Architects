package com.cybersquad.murendeni.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Lists every API call the app can make. Retrofit builds the working code from this list.
interface ApiService {

    // POST {BASE_URL}auth/login
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // POST {BASE_URL}auth/register
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
}