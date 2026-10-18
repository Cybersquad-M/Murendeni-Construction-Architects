package com.cybersquad.murendeni.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.cybersquad.murendeni.LoginActivity
import com.cybersquad.murendeni.data.SessionManager

// Used when the API answers 401, which means the saved login token has expired.
// Clears the saved login and returns to the login screen.
fun Context.returnToLogin(message: String? = null) {
    SessionManager(this).clear()

    if (message != null) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    val intent = Intent(this, LoginActivity::class.java)
    // Closes every other screen, so the back button cannot return to a logged-in screen.
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(intent)
}