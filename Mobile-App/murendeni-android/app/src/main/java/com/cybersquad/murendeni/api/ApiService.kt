package com.cybersquad.murendeni.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// Lists every API call the app can make. Retrofit builds the working code from this list.
interface ApiService {

    // ----- Login and account -----

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("account")
    suspend fun getAccount(): Response<AccountInfo>

    @PUT("account")
    suspend fun updateAccount(@Body request: UpdateAccountRequest): Response<AccountInfo>

    @POST("account/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    // ----- Projects (the server filters the list by the caller's role) -----

    @GET("projects")
    suspend fun getProjects(): Response<List<ProjectSummary>>

    @GET("projects/{id}")
    suspend fun getProject(@Path("id") id: Int): Response<ProjectDetail>

    // Designers (own projects) and sales staff (any project).
    @PUT("projects/{id}/status")
    suspend fun updateProjectStatus(
        @Path("id") id: Int,
        @Body request: UpdateProjectStatusRequest
    ): Response<StatusResponse>

    // Designers only. The file travels as a multipart form together with its title and status.
    @Multipart
    @POST("projects/{id}/files")
    suspend fun uploadFile(
        @Path("id") id: Int,
        @Part title: MultipartBody.Part,
        @Part status: MultipartBody.Part,
        @Part file: MultipartBody.Part
    ): Response<DesignFile>

    // ----- Client inquiries -----

    @GET("inquiries")
    suspend fun getInquiries(): Response<List<InquiryItem>>

    @POST("inquiries")
    suspend fun createInquiry(@Body request: CreateInquiryRequest): Response<InquiryItem>

    // ----- Sales staff -----

    // The status filter is optional. When it is null, every inquiry is returned.
    @GET("staff/inquiries")
    suspend fun getStaffInquiries(@Query("status") status: String?): Response<List<StaffInquiry>>

    @GET("staff/inquiries/{id}")
    suspend fun getStaffInquiry(@Path("id") id: Int): Response<StaffInquiry>

    @PUT("staff/inquiries/{id}")
    suspend fun updateStaffInquiry(
        @Path("id") id: Int,
        @Body request: UpdateInquiryRequest
    ): Response<StaffInquiry>

    @POST("staff/inquiries/{id}/create-client")
    suspend fun createClientFromInquiry(@Path("id") id: Int): Response<CreateClientResult>

    @GET("staff/clients")
    suspend fun getStaffClients(): Response<List<ClientItem>>

    @GET("staff/employees")
    suspend fun getStaffEmployees(): Response<List<EmployeeItem>>

    @POST("staff/projects")
    suspend fun createProject(@Body request: CreateProjectRequest): Response<CreatedProject>
}