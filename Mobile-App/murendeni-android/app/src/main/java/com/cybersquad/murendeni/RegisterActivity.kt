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
import com.cybersquad.murendeni.api.RegisterRequest
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityRegisterBinding
import com.cybersquad.murendeni.util.applySystemBarPadding
import kotlinx.coroutines.launch
import java.io.IOException

// Lets a new client create an account. The account is created on the server.
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        // The background is dark, so the status bar icons must be light to stay visible.
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = false

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvLoginLink.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        // The input is checked on the phone first, so obvious mistakes need no server call.
        if (fullName.isEmpty()) {
            showError("Please enter your full name.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.")
            return
        }
        if (password.length < 8) {
            showError("The password must be at least 8 characters long.")
            return
        }
        if (password != confirm) {
            showError("The passwords do not match.")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val request = RegisterRequest(fullName, email, phone.ifEmpty { null }, password)
                val response = ApiClient.getService(this@RegisterActivity).register(request)
                val auth = response.body()

                if (response.isSuccessful && auth != null) {
                    session.saveSession(auth.token, auth.userId, auth.fullName, auth.email, auth.role)
                    Log.d(TAG, "Registration worked.")

                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    // Closes this screen and the login screen below it.
                    finishAffinity()
                } else {
                    Log.d(TAG, "Registration rejected. Code: ${response.code()}")
                    showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem during registration", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem during registration", e)
                showError("Something went wrong. Please try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
        if (loading) {
            binding.tvError.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}