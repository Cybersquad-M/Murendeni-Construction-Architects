package com.cybersquad.murendeni

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.ChangePasswordRequest
import com.cybersquad.murendeni.databinding.ActivityChangePasswordBinding
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.launch
import java.io.IOException

// Lets any logged-in user (client or staff) change their password.
class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        binding.tvBack.setOnClickListener { finish() }
        binding.btnChange.setOnClickListener { changePassword() }
    }

    private fun changePassword() {
        val current = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        // The input is checked on the phone first, so obvious mistakes need no server call.
        if (current.isEmpty()) {
            showError("Please enter your current password.")
            return
        }
        if (newPassword.length < 8) {
            showError("The new password must be at least 8 characters long.")
            return
        }
        if (newPassword != confirm) {
            showError("The new passwords do not match.")
            return
        }
        if (newPassword == current) {
            showError("The new password must be different from the current one.")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(current, newPassword)
                val response = ApiClient.getService(this@ChangePasswordActivity).changePassword(request)

                when {
                    // A wrong current password is a 400 from the server, so a 401 here
                    // can only mean that the saved login has expired.
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful -> {
                        Toast.makeText(this@ChangePasswordActivity, "Your password has been changed.", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while changing the password", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while changing the password", e)
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
        binding.btnChange.isEnabled = !loading
        if (loading) {
            binding.tvError.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "ChangePasswordActivity"
    }
}