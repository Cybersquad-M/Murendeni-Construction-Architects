package com.cybersquad.murendeni.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Lists every API call the app can make. Retrofit builds the working code from this list.
interface ApiService {

    // POST {BASE_URL}auth/login
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // POST {BASE_URL}auth/register
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // GET {BASE_URL}projects (the server filters the list by the caller's role)
    @GET("projects")
    suspend fun getProjects(): Response<List<ProjectSummary>>

    // GET {BASE_URL}projects/5
    @GET("projects/{id}")
    suspend fun getProject(@Path("id") id: Int): Response<ProjectDetail>
}