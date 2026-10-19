package com.cybersquad.murendeni

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.CreateInquiryRequest
import com.cybersquad.murendeni.databinding.ActivityNewInquiryBinding
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.launch
import java.io.IOException

// Lets a client send a new inquiry to the company.
class NewInquiryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewInquiryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewInquiryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        // The same services that are offered on the website's contact form.
        val services = listOf(
            "Architectural Drafting",
            "2D Floor Plans",
            "3D Building Models",
            "Design Services",
            "Other / Not sure"
        )
        binding.actvService.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, services)
        )

        binding.tvBack.setOnClickListener { finish() }
        binding.btnSend.setOnClickListener { sendInquiry() }
    }

    private fun sendInquiry() {
        val service = binding.actvService.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()

        if (message.isEmpty()) {
            showError("Please enter a message.")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val request = CreateInquiryRequest(service.ifEmpty { null }, message)
                val response = ApiClient.getService(this@NewInquiryActivity).createInquiry(request)

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful -> {
                        Toast.makeText(this@NewInquiryActivity, "Your inquiry has been sent.", Toast.LENGTH_SHORT).show()
                        finish() // The Inquiries tab reloads itself when it comes back into view.
                    }

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while sending the inquiry", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while sending the inquiry", e)
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
        binding.btnSend.isEnabled = !loading
        if (loading) {
            binding.tvError.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "NewInquiryActivity"
    }
}