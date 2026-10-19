package com.cybersquad.murendeni

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.DesignFile
import com.cybersquad.murendeni.api.ProjectDetail
import com.cybersquad.murendeni.api.UpdateProjectStatusRequest
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityProjectDetailsBinding
import com.cybersquad.murendeni.databinding.ItemDesignFileBinding
import com.cybersquad.murendeni.util.DateFormatter
import com.cybersquad.murendeni.util.StatusUi
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.launch
import java.io.IOException

// Shows one project: cover photo, details, status and the list of design files.
// Designers and sales staff can also change the status. Designers can upload files.
class ProjectDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectDetailsBinding
    private var projectId = 0
    private var role = ""

    // The labels shown on screen, and the matching values the API expects.
    private val statusLabels = listOf("Inquiry", "In Progress", "Review", "Completed")
    private val statusValues = listOf("Inquiry", "InProgress", "Review", "Completed")
    private var selectedStatusIndex = 0

    // Starts the upload screen. When a file was uploaded, the project reloads to show it.
    private val uploadLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadProject(silent = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProjectDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        role = SessionManager(this).getRole()

        // The id of the project to show is passed in by the list screen.
        projectId = intent.getIntExtra(EXTRA_PROJECT_ID, 0)

        binding.tvBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { loadProject() }

        // The status dropdown is set up once. Its starting choice is set when the project loads.
        binding.actvStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusLabels)
        )
        binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatusIndex = position
        }
        binding.btnSaveStatus.setOnClickListener { saveStatus() }

        binding.btnUpload.setOnClickListener {
            val intent = Intent(this, UploadFileActivity::class.java)
            intent.putExtra(UploadFileActivity.EXTRA_PROJECT_ID, projectId)
            intent.putExtra(UploadFileActivity.EXTRA_PROJECT_NAME, binding.tvName.text.toString())
            uploadLauncher.launch(intent)
        }

        loadProject()
    }

    // A silent load refreshes the screen in place after a change. The page is not
    // hidden, so the scroll position is kept, and a failure is shown as a short message.
    private fun loadProject(silent: Boolean = false) {
        if (!silent) {
            binding.progress.visibility = View.VISIBLE
            binding.errorLayout.visibility = View.GONE
            binding.scrollContent.visibility = View.GONE
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@ProjectDetailsActivity).getProject(projectId)
                val project = response.body()

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && project != null -> showProject(project)

                    else -> reportProblem(ApiErrors.messageFrom(response), silent)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while loading the project", e)
                reportProblem("Could not reach the server. Check the internet connection and try again.", silent)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while loading the project", e)
                reportProblem("Something went wrong. Please try again.", silent)
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun showProject(project: ProjectDetail) {
        binding.scrollContent.visibility = View.VISIBLE

        binding.tvName.text = project.name
        binding.tvType.text = project.propertyType
        binding.tvStatus.text = StatusUi.projectLabel(project.status)
        binding.tvStatus.backgroundTintList =
            ColorStateList.valueOf(StatusUi.projectColor(project.status))

        // The info lines are built from whatever values exist. Empty ones are skipped.
        val lines = mutableListOf<String>()
        if (role != "Client") project.clientName?.let { lines.add("Client: $it") }
        project.designerName?.let { lines.add("Designer: $it") }
        project.location?.let { lines.add("Location: $it") }
        project.yearCompleted?.let { lines.add("Year: $it") }
        lines.add("Last updated: ${DateFormatter.format(project.updatedAt)}")
        binding.tvInfo.text = lines.joinToString("\n")

        if (project.description.isNullOrBlank()) {
            binding.tvDescription.visibility = View.GONE
        } else {
            binding.tvDescription.text = project.description
            binding.tvDescription.visibility = View.VISIBLE
        }

        Glide.with(this)
            .load(project.coverImageUrl)
            .centerCrop()
            .placeholder(R.color.placeholder_gray)
            .fallback(R.color.placeholder_gray)
            .error(R.color.placeholder_gray)
            .into(binding.ivCover)

        // Designers and sales staff can change the status. Clients can only look.
        val canChangeStatus = role == "Designer" || role == "Sales"
        binding.cardStatus.visibility = if (canChangeStatus) View.VISIBLE else View.GONE
        if (canChangeStatus) {
            selectedStatusIndex = statusValues.indexOf(project.status).coerceAtLeast(0)
            binding.actvStatus.setText(statusLabels[selectedStatusIndex], false)
        }

        // Only designers upload files.
        binding.btnUpload.visibility = if (role == "Designer") View.VISIBLE else View.GONE

        showFiles(project.files)
    }

    private fun saveStatus() {
        binding.btnSaveStatus.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = UpdateProjectStatusRequest(statusValues[selectedStatusIndex])
                val response = ApiClient.getService(this@ProjectDetailsActivity)
                    .updateProjectStatus(projectId, request)

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful -> {
                        Toast.makeText(this@ProjectDetailsActivity, "Status updated.", Toast.LENGTH_SHORT).show()
                        loadProject(silent = true)
                    }

                    else -> Toast.makeText(
                        this@ProjectDetailsActivity,
                        ApiErrors.messageFrom(response),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while saving the status", e)
                Toast.makeText(
                    this@ProjectDetailsActivity,
                    "Could not reach the server. Check the internet connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while saving the status", e)
                Toast.makeText(this@ProjectDetailsActivity, "Something went wrong. Please try again.", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSaveStatus.isEnabled = true
            }
        }
    }

    // Adds one card per design file, each with View and Download buttons.
    private fun showFiles(files: List<DesignFile>) {
        binding.filesContainer.removeAllViews()
        binding.tvNoFiles.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE

        for (file in files) {
            val row = ItemDesignFileBinding.inflate(layoutInflater, binding.filesContainer, false)

            row.tvFileTitle.text = file.title
            row.tvFileStatus.text = StatusUi.fileLabel(file.status)
            row.tvFileStatus.backgroundTintList =
                ColorStateList.valueOf(StatusUi.fileColor(file.status))
            row.tvFileDate.text = DateFormatter.format(file.createdAt)

            val url = file.fileUrl
            if (url == null) {
                row.btnView.isEnabled = false
                row.btnDownload.isEnabled = false
            } else {
                row.btnView.setOnClickListener { openFile(url) }
                row.btnDownload.setOnClickListener { downloadFile(file.title, url) }
            }

            binding.filesContainer.addView(row.root)
        }
    }

    // Opens the file in the browser, which shows images and PDFs directly.
    private fun openFile(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app was found that can open this file.", Toast.LENGTH_LONG).show()
        }
    }

    // Saves the file into the phone's Downloads folder, using Android's download manager.
    // This works without extra permission on Android 10 and newer.
    private fun downloadFile(title: String, url: String) {
        try {
            // The stored name looks like "<id>_Floor Plan.pdf". The part before
            // the first underscore is removed to give a clean file name.
            val storedName = Uri.parse(url).lastPathSegment ?: "design-file"
            val fileName = if (storedName.contains('_')) storedName.substringAfter('_') else storedName

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(this, "Download started. Check the notifications.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "The download could not be started", e)
            Toast.makeText(this, "The download could not be started.", Toast.LENGTH_LONG).show()
        }
    }

    // A full load that fails replaces the screen with an error and a retry button.
    // A silent load that fails only shows a short message.
    private fun reportProblem(message: String, silent: Boolean) {
        if (silent) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } else {
            binding.tvError.text = message
            binding.errorLayout.visibility = View.VISIBLE
        }
    }

    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
        private const val TAG = "ProjectDetailsActivity"
    }
}