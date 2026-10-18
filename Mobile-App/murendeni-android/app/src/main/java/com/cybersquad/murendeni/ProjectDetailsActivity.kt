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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.DesignFile
import com.cybersquad.murendeni.api.ProjectDetail
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
class ProjectDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectDetailsBinding
    private var projectId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProjectDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        // The id of the project to show is passed in by the list screen.
        projectId = intent.getIntExtra(EXTRA_PROJECT_ID, 0)

        binding.tvBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { loadProject() }

        loadProject()
    }

    private fun loadProject() {
        binding.progress.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        binding.scrollContent.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@ProjectDetailsActivity).getProject(projectId)
                val project = response.body()

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && project != null -> showProject(project)

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while loading the project", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while loading the project", e)
                showError("Something went wrong. Please try again.")
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
        val isClient = SessionManager(this).getRole() == "Client"
        val lines = mutableListOf<String>()
        if (!isClient) project.clientName?.let { lines.add("Client: $it") }
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

        showFiles(project.files)
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

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.errorLayout.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
        private const val TAG = "ProjectDetailsActivity"
    }
}