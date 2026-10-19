package com.cybersquad.murendeni.data

import android.content.Context

// Remembers who is logged in, using the phone's private storage.
// Only this app can read these values.
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("murendeni_session", Context.MODE_PRIVATE)

    fun saveSession(token: String, userId: Int, fullName: String, email: String, role: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_NAME, fullName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getName(): String = prefs.getString(KEY_NAME, "") ?: ""
    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""

    fun isLoggedIn(): Boolean = getToken() != null

    // Used when the user logs out or the token has expired.
    fun clear() {
        prefs.edit().clear().apply()
    }

    // Used after the user edits their name in Settings.
    fun updateName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"
    }
}