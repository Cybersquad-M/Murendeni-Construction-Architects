package com.cybersquad.murendeni.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Lists every API call the app can make. Retrofit builds the working code from this list.
interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // The server filters the project list by the caller's role.
    @GET("projects")
    suspend fun getProjects(): Response<List<ProjectSummary>>

    @GET("projects/{id}")
    suspend fun getProject(@Path("id") id: Int): Response<ProjectDetail>

    // Inquiries belong to clients only.
    @GET("inquiries")
    suspend fun getInquiries(): Response<List<InquiryItem>>

    @POST("inquiries")
    suspend fun createInquiry(@Body request: CreateInquiryRequest): Response<InquiryItem>

    @GET("account")
    suspend fun getAccount(): Response<AccountInfo>

    @PUT("account")
    suspend fun updateAccount(@Body request: UpdateAccountRequest): Response<AccountInfo>

    @POST("account/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>
}