package com.cybersquad.murendeni

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.CreateClientResult
import com.cybersquad.murendeni.api.EmployeeItem
import com.cybersquad.murendeni.api.StaffInquiry
import com.cybersquad.murendeni.api.UpdateInquiryRequest
import com.cybersquad.murendeni.databinding.ActivityStaffInquiryDetailBinding
import com.cybersquad.murendeni.util.DateFormatter
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.cybersquad.murendeni.util.returnToLogin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.IOException

// Shows one inquiry to sales staff. Sales can change its status, assign it,
// and turn a guest inquiry into a client account.
class StaffInquiryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffInquiryDetailBinding
    private var inquiryId = 0
    private var inquiry: StaffInquiry? = null

    // The labels shown on screen, and the matching values the API expects.
    private val statusLabels = listOf("Open", "In Progress", "Resolved", "Closed")
    private val statusValues = listOf("Open", "InProgress", "Resolved", "Closed")

    private var assigneeOptions: List<AssigneeOption> = emptyList()
    private var selectedStatusIndex = 0
    private var selectedAssigneeIndex = 0

    // One entry in the "Assign to" list. A null id means "not assigned".
    private class AssigneeOption(val id: Int?, val label: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStaffInquiryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        inquiryId = intent.getIntExtra(EXTRA_INQUIRY_ID, 0)

        binding.tvBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { loadInquiry() }
        binding.btnSave.setOnClickListener { saveChanges() }
        binding.btnCreateAccount.setOnClickListener { confirmCreateAccount() }

        loadInquiry()
    }

    // Loads the inquiry and the list of staff to assign it to.
    private fun loadInquiry() {
        binding.progress.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        binding.scrollContent.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService(this@StaffInquiryDetailActivity)
                val inquiryResponse = service.getStaffInquiry(inquiryId)
                val employeesResponse = service.getStaffEmployees()

                val loaded = inquiryResponse.body()
                val employees = employeesResponse.body()

                when {
                    inquiryResponse.code() == 401 || employeesResponse.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    inquiryResponse.isSuccessful && loaded != null &&
                            employeesResponse.isSuccessful && employees != null ->
                        showInquiry(loaded, employees)

                    !inquiryResponse.isSuccessful ->
                        showLoadError(ApiErrors.messageFrom(inquiryResponse))

                    else -> showLoadError(ApiErrors.messageFrom(employeesResponse))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while loading the inquiry", e)
                showLoadError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while loading the inquiry", e)
                showLoadError("Something went wrong. Please try again.")
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun showInquiry(loaded: StaffInquiry, employees: List<EmployeeItem>) {
        inquiry = loaded
        binding.scrollContent.visibility = View.VISIBLE

        binding.tvContactName.text = loaded.contactName
        binding.tvContactEmail.text = loaded.contactEmail ?: "No email given"
        binding.tvContactPhone.text = loaded.contactPhone ?: "No phone number given"
        binding.tvService.text = "Service: ${loaded.service ?: "Not specified"}"
        binding.tvReceived.text = "Received: ${DateFormatter.format(loaded.createdAt)}"
        binding.tvMessage.text = loaded.message ?: ""

        // Status dropdown, starting on the inquiry's current status.
        selectedStatusIndex = statusValues.indexOf(loaded.status).coerceAtLeast(0)
        binding.actvStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusLabels)
        )
        binding.actvStatus.setText(statusLabels[selectedStatusIndex], false)
        binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatusIndex = position
        }

        // "Assign to" dropdown: "Not assigned" first, then the staff members.
        val options = mutableListOf(AssigneeOption(null, "Not assigned"))
        for (employee in employees) {
            options.add(AssigneeOption(employee.employeeId, "${employee.fullName} (${employee.role})"))
        }

        // An Admin can be the assignee when set on the website. Admins are not in the staff list,
        // so the current assignee is added here. Otherwise saving would silently remove them.
        val currentId = loaded.assignedToId
        if (currentId != null && options.none { it.id == currentId }) {
            val name = loaded.assignedToName ?: "Current assignee"
            options.add(AssigneeOption(currentId, "$name (current)"))
        }

        assigneeOptions = options
        selectedAssigneeIndex = options.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        binding.actvAssignee.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options.map { it.label })
        )
        binding.actvAssignee.setText(options[selectedAssigneeIndex].label, false)
        binding.actvAssignee.setOnItemClickListener { _, _, position, _ ->
            selectedAssigneeIndex = position
        }

        showAccountSection(loaded)
    }

    // Shows the "Create client account" button, or a note that an account is already linked.
    private fun showAccountSection(current: StaffInquiry) {
        when {
            current.hasClientAccount -> {
                binding.tvAccountLinked.visibility = View.VISIBLE
                binding.btnCreateAccount.visibility = View.GONE
            }
            !current.contactEmail.isNullOrBlank() -> {
                binding.tvAccountLinked.visibility = View.GONE
                binding.btnCreateAccount.visibility = View.VISIBLE
            }
            else -> {
                // Without an email address, an account cannot be created.
                binding.tvAccountLinked.visibility = View.GONE
                binding.btnCreateAccount.visibility = View.GONE
            }
        }
    }

    private fun saveChanges() {
        val request = UpdateInquiryRequest(
            statusValues[selectedStatusIndex],
            assigneeOptions[selectedAssigneeIndex].id
        )

        binding.tvError.visibility = View.GONE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@StaffInquiryDetailActivity)
                    .updateStaffInquiry(inquiryId, request)
                val updated = response.body()

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && updated != null -> {
                        inquiry = updated
                        Toast.makeText(this@StaffInquiryDetailActivity, "Changes saved.", Toast.LENGTH_SHORT).show()
                    }

                    else -> showSaveError(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while saving the inquiry", e)
                showSaveError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while saving the inquiry", e)
                showSaveError("Something went wrong. Please try again.")
            } finally {
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun confirmCreateAccount() {
        val current = inquiry ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle("Create client account")
            .setMessage(
                "Create an account for ${current.contactName} (${current.contactEmail})? " +
                        "A temporary password will be generated."
            )
            .setPositiveButton("Create") { _, _ -> createAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createAccount() {
        binding.btnCreateAccount.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@StaffInquiryDetailActivity)
                    .createClientFromInquiry(inquiryId)
                val result = response.body()

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && result != null -> {
                        inquiry = inquiry?.copy(hasClientAccount = true)
                        inquiry?.let { showAccountSection(it) }
                        showAccountResult(result)
                    }

                    else -> Toast.makeText(
                        this@StaffInquiryDetailActivity,
                        ApiErrors.messageFrom(response),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while creating the account", e)
                Toast.makeText(
                    this@StaffInquiryDetailActivity,
                    "Could not reach the server. Check the internet connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while creating the account", e)
                Toast.makeText(this@StaffInquiryDetailActivity, "Something went wrong. Please try again.", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnCreateAccount.isEnabled = true
            }
        }
    }

    // A new account shows its temporary password once. It is never stored in readable form.
    private fun showAccountResult(result: CreateClientResult) {
        val password = result.temporaryPassword

        if (password == null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Done")
                .setMessage(result.message)
                .setPositiveButton("OK", null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Account created")
            .setMessage(
                "${result.message}\n\nEmail: ${result.email}\nTemporary password: $password" +
                        "\n\nThis password is only shown once."
            )
            .setPositiveButton("Copy password") { _, _ -> copyToClipboard(password) }
            .setNegativeButton("Close", null)
            .setCancelable(false) // A tap outside the box cannot dismiss it by accident.
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Temporary password", text))
        Toast.makeText(this, "Password copied.", Toast.LENGTH_SHORT).show()
    }

    private fun showSaveError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun showLoadError(message: String) {
        binding.tvLoadError.text = message
        binding.errorLayout.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_INQUIRY_ID = "inquiry_id"
        private const val TAG = "StaffInquiryDetail"
    }
}