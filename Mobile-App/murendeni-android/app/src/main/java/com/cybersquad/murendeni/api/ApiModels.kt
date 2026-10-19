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

// One inquiry in the Inquiries tab.
data class InquiryItem(
    val inquiryId: Int,
    val subject: String,
    val service: String?,
    val message: String?,
    val status: String,           // "Open", "InProgress", "Resolved" or "Closed"
    val createdAt: String
)

// Data sent to the API to create an inquiry.
data class CreateInquiryRequest(
    val service: String?,
    val message: String
)

// The account details shown on the edit profile screen.
data class AccountInfo(
    val fullName: String,
    val email: String,
    val phone: String?,
    val role: String
)

// Data sent to the API to update a client's name and phone.
data class UpdateAccountRequest(
    val fullName: String,
    val phone: String?
)

// Data sent to the API to change a password.
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

// The short confirmation the API sends back after a password change.
data class MessageResponse(
    val message: String
)