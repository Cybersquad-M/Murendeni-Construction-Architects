package com.cybersquad.murendeni

import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.AuthResponse
import com.cybersquad.murendeni.api.CreateProjectRequest
import com.cybersquad.murendeni.api.LoginRequest
import com.cybersquad.murendeni.api.ProjectDetail
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

// Tests that error answers from the API are turned into clear messages for the user.
class ApiErrorsTest {

    // Builds an error answer like the ones the website API sends.
    private fun errorResponse(code: Int, body: String): Response<Any> {
        return Response.error(code, body.toResponseBody("application/json".toMediaType()))
    }

    @Test
    fun usesTheMessageSentByTheApi() {
        val response = errorResponse(401, """{"message":"Incorrect email or password."}""")

        assertEquals("Incorrect email or password.", ApiErrors.messageFrom(response))
    }

    @Test
    fun usesTheFirstValidationError() {
        val response = errorResponse(
            400,
            """{"errors":{"Email":["The Email field is not a valid e-mail address."]}}"""
        )

        assertEquals("The Email field is not a valid e-mail address.", ApiErrors.messageFrom(response))
    }

    @Test
    fun explainsAServerProblem() {
        assertEquals(
            "The server had a problem. Please try again.",
            ApiErrors.messageFrom(errorResponse(500, ""))
        )
    }

    @Test
    fun explainsAMissingItem() {
        assertEquals("That could not be found.", ApiErrors.messageFrom(errorResponse(404, "")))
    }

    @Test
    fun explainsMissingPermission() {
        assertEquals(
            "You do not have permission to do that.",
            ApiErrors.messageFrom(errorResponse(403, ""))
        )
    }

    @Test
    fun explainsAnExpiredLoginWhenTheBodyIsNotJson() {
        assertEquals(
            "Your login has expired. Please log in again.",
            ApiErrors.messageFrom(errorResponse(401, "oops"))
        )
    }

    @Test
    fun showsTheCodeForAnUnknownProblem() {
        assertEquals("Something went wrong (code 418).", ApiErrors.messageFrom(errorResponse(418, "")))
    }
}

// Tests that the app's data classes match the JSON that the website API sends and receives.
class ApiModelsTest {

    private val gson = Gson()

    @Test
    fun loginAnswerIsReadFromJson() {
        val json = """{"token":"abc123","userId":7,"fullName":"Test Client","email":"test@example.com","role":"Client"}"""

        val auth = gson.fromJson(json, AuthResponse::class.java)

        assertEquals("abc123", auth.token)
        assertEquals(7, auth.userId)
        assertEquals("Test Client", auth.fullName)
        assertEquals("test@example.com", auth.email)
        assertEquals("Client", auth.role)
    }

    @Test
    fun projectDetailIsReadFromJsonIncludingFilesAndEmptyValues() {
        val json = """
            {"projectId":3,"name":"Modern House","propertyType":"Residential","status":"InProgress",
             "location":null,"coverImageUrl":null,"clientName":"Test Client","designerName":"Designer One",
             "updatedAt":"2026-10-18T09:12:00Z","description":"A test project","yearCompleted":null,
             "files":[{"fileId":1,"title":"Floor plan","status":"draft",
                       "fileUrl":"https://example.com/uploads/plan.pdf","createdAt":"2026-10-18T09:00:00Z"}]}
        """.trimIndent()

        val project = gson.fromJson(json, ProjectDetail::class.java)

        assertEquals("Modern House", project.name)
        assertEquals("InProgress", project.status)
        assertNull(project.location)
        assertNull(project.yearCompleted)
        assertEquals(1, project.files.size)
        assertEquals("Floor plan", project.files[0].title)
    }

    @Test
    fun loginRequestUsesTheNamesTheApiExpects() {
        val json = gson.toJson(LoginRequest("a@b.com", "secret123"))

        assertTrue(json.contains("\"email\":\"a@b.com\""))
        assertTrue(json.contains("\"password\":\"secret123\""))
    }

    @Test
    fun createProjectRequestUsesTheNamesTheApiExpects() {
        val request = CreateProjectRequest(
            clientId = 1,
            employeeId = 2,
            name = "Villa",
            propertyType = "Residential",
            description = null,
            location = null,
            yearCompleted = null
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"clientId\":1"))
        assertTrue(json.contains("\"employeeId\":2"))
        assertTrue(json.contains("\"propertyType\":\"Residential\""))
    }
}