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
// One row in the project list.
data class ProjectSummary(
    val projectId: Int,
    val name: String,
    val propertyType: String,     // "Residential" or "Commercial"
    val status: String,           // "Inquiry", "InProgress", "Review" or "Completed"
    val location: String?,
    val coverImageUrl: String?,
    val clientName: String?,
    val designerName: String?,
    val updatedAt: String         // UTC date text, for example "2026-10-18T09:12:00Z"
)

// One design file inside a project.
data class DesignFile(
    val fileId: Int,
    val title: String,
    val status: String,           // "draft", "review", "approved" or "archived"
    val fileUrl: String?,
    val createdAt: String
)

// The full project screen: the summary details plus the description and files.
data class ProjectDetail(
    val projectId: Int,
    val name: String,
    val propertyType: String,
    val status: String,
    val location: String?,
    val coverImageUrl: String?,
    val clientName: String?,
    val designerName: String?,
    val updatedAt: String,
    val description: String?,
    val yearCompleted: Int?,
    val files: List<DesignFile>
)