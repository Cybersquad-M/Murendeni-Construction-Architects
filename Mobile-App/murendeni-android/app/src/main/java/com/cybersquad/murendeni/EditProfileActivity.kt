package com.cybersquad.murendeni

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.UpdateAccountRequest
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityEditProfileBinding
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.launch
import java.io.IOException

// Lets a client change their name and phone number. The email cannot be changed.
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        session = SessionManager(this)

        // The saved name and email show straight away. The phone number is fetched from the server below.
        binding.etFullName.setText(session.getName())
        binding.etEmail.setText(session.getEmail())

        binding.tvBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveProfile() }

        // Saving stays switched off until the details have loaded.
        // Otherwise a quick tap could save an empty phone number over the real one.
        binding.btnSave.isEnabled = false
        loadAccount()
    }

    private fun loadAccount() {
        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@EditProfileActivity).getAccount()
                val account = response.body()

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && account != null -> {
                        binding.etFullName.setText(account.fullName)
                        binding.etPhone.setText(account.phone ?: "")
                        binding.btnSave.isEnabled = true
                    }

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while loading the account", e)
                showError("Could not load your details. Check the internet connection, go back and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while loading the account", e)
                showError("Something went wrong. Please go back and try again.")
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (name.isEmpty()) {
            showError("Please enter your name.")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val request = UpdateAccountRequest(name, phone.ifEmpty { null })
                val response = ApiClient.getService(this@EditProfileActivity).updateAccount(request)
                val account = response.body()

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && account != null -> {
                        // Keeps the name saved on the phone in step with the server.
                        session.updateName(account.fullName)
                        Toast.makeText(this@EditProfileActivity, "Your details have been saved.", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while saving the account", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while saving the account", e)
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
        binding.btnSave.isEnabled = !loading
        if (loading) {
            binding.tvError.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "EditProfileActivity"
    }
}