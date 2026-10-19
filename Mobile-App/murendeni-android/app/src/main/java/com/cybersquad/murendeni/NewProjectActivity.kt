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
import com.cybersquad.murendeni.api.ClientItem
import com.cybersquad.murendeni.api.CreateProjectRequest
import com.cybersquad.murendeni.api.EmployeeItem
import com.cybersquad.murendeni.databinding.ActivityNewProjectBinding
import com.cybersquad.murendeni.util.applySystemBarPadding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.launch
import java.io.IOException

// Lets sales staff create a project for a client and assign a designer to it.
class NewProjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewProjectBinding

    private var clients: List<ClientItem> = emptyList()
    private var designers: List<EmployeeItem> = emptyList()
    private var selectedClientIndex = -1
    private var selectedDesignerIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewProjectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        // The two property types are fixed, so this dropdown is filled straight away.
        val types = listOf("Residential", "Commercial")
        binding.actvType.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types))
        binding.actvType.setText(types[0], false)

        binding.tvBack.setOnClickListener { finish() }
        binding.tvRetry.setOnClickListener { loadOptions() }
        binding.btnCreate.setOnClickListener { createProject() }

        loadOptions()
    }

    // Loads the clients and designers for the two dropdowns.
    private fun loadOptions() {
        binding.tvError.visibility = View.GONE
        binding.tvRetry.visibility = View.GONE
        binding.btnCreate.isEnabled = false
        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService(this@NewProjectActivity)
                val clientsResponse = service.getStaffClients()
                val employeesResponse = service.getStaffEmployees()

                val loadedClients = clientsResponse.body()
                val loadedEmployees = employeesResponse.body()

                when {
                    clientsResponse.code() == 401 || employeesResponse.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    clientsResponse.isSuccessful && loadedClients != null &&
                            employeesResponse.isSuccessful && loadedEmployees != null ->
                        showOptions(loadedClients, loadedEmployees.filter { it.role == "Designer" })

                    !clientsResponse.isSuccessful -> showLoadProblem(ApiErrors.messageFrom(clientsResponse))

                    else -> showLoadProblem(ApiErrors.messageFrom(employeesResponse))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while loading the lists", e)
                showLoadProblem("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while loading the lists", e)
                showLoadProblem("Something went wrong. Please try again.")
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun showOptions(loadedClients: List<ClientItem>, loadedDesigners: List<EmployeeItem>) {
        clients = loadedClients
        designers = loadedDesigners
        selectedClientIndex = -1
        selectedDesignerIndex = -1

        binding.actvClient.setText("", false)
        binding.actvDesigner.setText("", false)

        binding.actvClient.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                clients.map { "${it.fullName} (${it.email})" }
            )
        )
        binding.actvDesigner.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, designers.map { it.fullName })
        )

        binding.actvClient.setOnItemClickListener { _, _, position, _ -> selectedClientIndex = position }
        binding.actvDesigner.setOnItemClickListener { _, _, position, _ -> selectedDesignerIndex = position }

        // A project needs both a client and a designer, so nothing can be created without them.
        when {
            clients.isEmpty() -> showProblem(
                "There are no clients yet. Create an account from an inquiry first."
            )
            designers.isEmpty() -> showProblem(
                "There are no designers yet. Ask an Admin to add one on the website."
            )
            else -> binding.btnCreate.isEnabled = true
        }
    }

    private fun createProject() {
        val name = binding.etName.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val yearText = binding.etYear.text.toString().trim()
        val year = yearText.toIntOrNull()
        val propertyType = binding.actvType.text.toString().ifBlank { "Residential" }

        // The input is checked on the phone first, so obvious mistakes need no server call.
        if (selectedClientIndex < 0) {
            showProblem("Please choose a client.")
            return
        }
        if (selectedDesignerIndex < 0) {
            showProblem("Please choose a designer.")
            return
        }
        if (name.isEmpty()) {
            showProblem("Please enter a project name.")
            return
        }
        if (yearText.isNotEmpty() && (year == null || year !in 1900..2100)) {
            showProblem("Please enter a year between 1900 and 2100, or leave it empty.")
            return
        }

        val request = CreateProjectRequest(
            clientId = clients[selectedClientIndex].clientId,
            employeeId = designers[selectedDesignerIndex].employeeId,
            name = name,
            propertyType = propertyType,
            description = description.ifEmpty { null },
            location = location.ifEmpty { null },
            yearCompleted = year
        )

        binding.tvError.visibility = View.GONE
        binding.btnCreate.isEnabled = false
        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@NewProjectActivity).createProject(request)

                when {
                    response.code() == 401 ->
                        returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful -> {
                        Toast.makeText(this@NewProjectActivity, "Project created.", Toast.LENGTH_SHORT).show()
                        finish() // The Projects tab reloads itself when it comes back into view.
                    }

                    else -> showProblem(ApiErrors.messageFrom(response))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while creating the project", e)
                showProblem("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while creating the project", e)
                showProblem("Something went wrong. Please try again.")
            } finally {
                binding.progress.visibility = View.GONE
                // The button stays off when there is nothing valid to choose from.
                binding.btnCreate.isEnabled = clients.isNotEmpty() && designers.isNotEmpty()
            }
        }
    }

    private fun showProblem(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    // Used when the lists could not be loaded. A "Try again" link is shown with the message.
    private fun showLoadProblem(message: String) {
        showProblem(message)
        binding.tvRetry.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "NewProjectActivity"
    }
}