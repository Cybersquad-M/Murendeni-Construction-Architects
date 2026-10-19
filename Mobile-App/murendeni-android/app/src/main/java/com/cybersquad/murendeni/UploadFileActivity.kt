package com.cybersquad.murendeni

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.UriRequestBody
import com.cybersquad.murendeni.databinding.ActivityUploadFileBinding
import com.cybersquad.murendeni.util.FileUtils
import com.cybersquad.murendeni.util.PickedFile
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.IOException

// Lets a designer upload a design file to one of their projects.
class UploadFileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadFileBinding
    private var projectId = 0
    private var pickedFile: PickedFile? = null

    // The labels shown on screen, and the matching values the API expects.
    private val statusLabels = listOf("Draft", "Review", "Approved", "Archived")
    private val statusValues = listOf("draft", "review", "approved", "archived")
    private var selectedStatusIndex = 0

    // These limits match the rules on the server, which checks them again.
    private val allowedExtensions =
        listOf("pdf", "png", "jpg", "jpeg", "webp", "dwg", "dxf", "rvt", "skp", "zip")
    private val maxFileBytes = 25L * 1024 * 1024

    // Opens the phone's file picker. The chosen file comes back as a Uri, or null when cancelled.
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            handlePickedFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUploadFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        projectId = intent.getIntExtra(EXTRA_PROJECT_ID, 0)
        binding.tvProjectName.text = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: ""

        binding.actvStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusLabels)
        )
        binding.actvStatus.setText(statusLabels[selectedStatusIndex], false)
        binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatusIndex = position
        }

        binding.tvBack.setOnClickListener { finish() }
        binding.btnChoose.setOnClickListener { filePicker.launch(arrayOf("*/*")) }
        binding.btnUpload.setOnClickListener { upload() }
    }

    private fun handlePickedFile(uri: Uri) {
        val file = try {
            FileUtils.describe(this, uri)
        } catch (e: Exception) {
            Log.e(TAG, "The picked file could not be read", e)
            showError("That file could not be read. Please choose another one.")
            return
        }

        pickedFile = file

        val sizeText = if (file.size >= 0) " (${FileUtils.formatSize(file.size)})" else ""
        binding.tvFileInfo.text = "${file.name}$sizeText"
        binding.tvError.visibility = View.GONE

        // The title starts as the file name without its ending, when no title was typed yet.
        if (binding.etTitle.text.isNullOrBlank()) {
            binding.etTitle.setText(file.name.substringBeforeLast('.'))
        }
    }

    private fun upload() {
        val file = pickedFile
        val title = binding.etTitle.text.toString().trim()

        // The input is checked on the phone first, so obvious mistakes need no server call.
        if (file == null) {
            showError("Please choose a file first.")
            return
        }
        if (title.isEmpty()) {
            showError("Please enter a title for the file.")
            return
        }

        val extension = file.name.substringAfterLast('.', "").lowercase()
        if (extension !in allowedExtensions) {
            showError("This file type is not allowed. Allowed types: ${allowedExtensions.joinToString(", ").uppercase()}.")
            return
        }
        if (file.size > maxFileBytes) {
            showError("The file is too large. The limit is 25 MB.")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val fileBody = UriRequestBody(applicationContext, file.uri, file.mimeType, file.size)

                val response = ApiClient.getService(this@UploadFileActivity).uploadFile(
                    projectId,
                    MultipartBody.Part.createFormData("title", title),
                    MultipartBody.Part.createFormData("status", statusValues[selectedStatusIndex]),
                    MultipartBody.Part.createFormData("file", file.name, fileBody)
                )

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful -> {
                        Toast.makeText(this@UploadFileActivity, "File uploaded.", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK) // Tells the project screen to reload and show the new file.
                        finish()
                    }

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "The upload failed", e)
                showError("The upload failed. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem during the upload", e)
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
        binding.btnUpload.isEnabled = !loading
        binding.btnChoose.isEnabled = !loading
        binding.btnUpload.text = if (loading) "Uploading..." else "Upload file"
        if (loading) {
            binding.tvError.visibility = View.GONE
        }
    }

    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_PROJECT_NAME = "project_name"
        private const val TAG = "UploadFileActivity"
    }
}