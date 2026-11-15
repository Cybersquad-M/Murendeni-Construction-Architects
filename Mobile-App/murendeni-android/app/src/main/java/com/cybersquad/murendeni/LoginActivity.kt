package com.cybersquad.murendeni

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.GoogleLoginRequest
import com.cybersquad.murendeni.api.LoginRequest
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityLoginBinding
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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
        binding.btnGoogle.setOnClickListener { attemptGoogleLogin() }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvHelpLink.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
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

    // "Continue with Google": the phone shows the Google account picker, and the ID token it
    // returns is sent to the website, which checks it and answers like a normal login.
    private fun attemptGoogleLogin() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = CredentialManager.create(this@LoginActivity)
                    .getCredential(this@LoginActivity, request)

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken

                    val response = ApiClient.getService(this@LoginActivity)
                        .loginWithGoogle(GoogleLoginRequest(idToken))
                    val auth = response.body()

                    if (response.isSuccessful && auth != null) {
                        session.saveSession(auth.token, auth.userId, auth.fullName, auth.email, auth.role)
                        Log.d(TAG, "Google login worked. Role: ${auth.role}")
                        openHome()
                    } else {
                        Log.d(TAG, "Google login rejected. Code: ${response.code()}")
                        showError(ApiErrors.messageFrom(response))
                    }
                } else {
                    showError("Google sign-in did not return an account. Please try again.")
                }
            } catch (e: GetCredentialCancellationException) {
                showError("Google sign-in was cancelled.")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google sign-in failed: ${e.type}", e)
                showError("Google sign-in failed. Check that a Google account is set up on this device.")
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Bad Google token", e)
                showError("Google sign-in failed. Please try again.")
            } catch (e: IOException) {
                Log.e(TAG, "Network problem during Google login", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem during Google login", e)
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
        binding.btnGoogle.isEnabled = !loading
        if (loading) {
            binding.tvError.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "LoginActivity"

        // The WEB client ID from Google Cloud Console (the same one the website uses),
        // NOT the Android client ID. It is not a secret.
        private const val GOOGLE_WEB_CLIENT_ID = "1015453521520-hsqs5uj8gjakrr2r3ltrgb4liscvus76.apps.googleusercontent.com"
    }
}
