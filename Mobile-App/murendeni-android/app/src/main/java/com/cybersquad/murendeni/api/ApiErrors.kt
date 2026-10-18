package com.cybersquad.murendeni.api

import org.json.JSONObject
import retrofit2.Response

// Turns an error answer from the API into a short message for the user.
object ApiErrors {

    fun messageFrom(response: Response<*>): String {
        val body = response.errorBody()?.string()

        if (!body.isNullOrBlank()) {
            try {
                val json = JSONObject(body)

                // Most errors look like {"message": "Incorrect email or password."}
                if (json.has("message")) {
                    return json.getString("message")
                }

                // Validation errors look like {"errors": {"Email": ["The Email field is not valid."]}}
                if (json.has("errors")) {
                    val errors = json.getJSONObject("errors")
                    val firstField = errors.keys().next()
                    return errors.getJSONArray(firstField).getString(0)
                }
            } catch (e: Exception) {
                // The body was not the expected JSON, so the general message below is used.
            }
        }

        return when (response.code()) {
            401 -> "Your login has expired. Please log in again."
            403 -> "You do not have permission to do that."
            404 -> "That could not be found."
            in 500..599 -> "The server had a problem. Please try again."
            else -> "Something went wrong (code ${response.code()})."
        }
    }
}