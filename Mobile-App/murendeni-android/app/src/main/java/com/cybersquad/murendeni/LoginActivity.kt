package com.cybersquad.murendeni

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.LoginRequest
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityLoginBinding
import com.cybersquad.murendeni.util.applySystemBarPadding
import kotlinx.coroutines.launch
import java.io.IOException

// The first screen. Logs the user in, or skips ahead when a login is already saved.
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        // A saved login means the user does not have to type the details again.
        if (session.isLoggedIn()) {
            openHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        // The background is dark, so the status bar icons must be light to stay visible.
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = false

        binding.btnLogin.setOnClickListener { attemptLogin() }

        // The Register screen is added in the next step, so this link does nothing yet.
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Input checks happen on the phone first, so obvious mistakes do not need a server call.
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.")
            return
        }
        if (password.isEmpty()) {
            showError("Please enter your password.")
            return
        }

        setLoading(true)

        // The API call runs in the background, so the screen stays responsive.
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@LoginActivity)
                    .login(LoginRequest(email, password))
                val auth = response.body()

                if (response.isSuccessful && auth != null) {
                    session.saveSession(auth.token, auth.userId, auth.fullName, auth.email, auth.role)
                    Log.d(TAG, "Login worked. Role: ${auth.role}")
                    openHome()
                } else {
                    Log.d(TAG, "Login rejected. Code: ${response.code()}")
                    showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem during login", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem during login", e)
                showError("Something went wrong. Please try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        if (loading) {
            binding.tvError.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}