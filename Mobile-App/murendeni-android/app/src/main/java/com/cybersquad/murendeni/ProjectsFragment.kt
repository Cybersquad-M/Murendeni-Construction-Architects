package com.cybersquad.murendeni

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.api.ApiErrors
import com.cybersquad.murendeni.api.ProjectSummary
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.FragmentProjectsBinding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

// The Projects tab. The server decides which projects belong in the list for each role.
class ProjectsFragment : Fragment() {

    private var _binding: FragmentProjectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProjectAdapter
    private var role = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        role = SessionManager(requireContext()).getRole()

        binding.tvTitle.text = if (role == "Sales") "All Projects" else "My Projects"

        // Only sales staff can create projects.
        binding.btnNewProject.visibility = if (role == "Sales") View.VISIBLE else View.GONE
        binding.btnNewProject.setOnClickListener {
            startActivity(Intent(requireContext(), NewProjectActivity::class.java))
        }

        // Staff see the client's name on each card. Clients do not need it.
        adapter = ProjectAdapter(showClient = role != "Client") { project -> openDetails(project) }
        binding.rvProjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProjects.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.brand_brick)
        binding.swipeRefresh.setOnRefreshListener { loadProjects() }
        binding.btnRetry.setOnClickListener { loadProjects() }
    }

    // Loads every time the screen comes back into view, so a new project or a
    // changed status shows straight away.
    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun loadProjects() {
        binding.swipeRefresh.isRefreshing = true
        binding.errorLayout.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.getService(requireContext()).getProjects()
                val projects = response.body()

                when {
                    // 401 means the saved login has expired.
                    response.code() == 401 ->
                        requireContext().returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && projects != null -> showProjects(projects)

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: CancellationException) {
                // The screen was closed while loading, for example when the user switched tabs.
                // Nothing needs to be shown, so the cancellation is passed on.
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while loading projects", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while loading projects", e)
                showError("Something went wrong. Please try again.")
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showProjects(projects: List<ProjectSummary>) {
        adapter.submit(projects)

        if (projects.isEmpty()) {
            binding.tvEmpty.text = if (role == "Client") {
                "You do not have any projects yet."
            } else {
                "No projects found."
            }
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.errorLayout.visibility = View.VISIBLE
    }

    private fun openDetails(project: ProjectSummary) {
        val intent = Intent(requireContext(), ProjectDetailsActivity::class.java)
        intent.putExtra(ProjectDetailsActivity.EXTRA_PROJECT_ID, project.projectId)
        startActivity(intent)
    }

    // The binding is cleared when the screen's view is destroyed, to avoid memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProjectsFragment"
    }
}