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
import com.cybersquad.murendeni.databinding.FragmentStaffInquiriesBinding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

// The Inquiries tab for sales staff: every inquiry, with a status filter.
class StaffInquiriesFragment : Fragment() {

    private var _binding: FragmentStaffInquiriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: StaffInquiryAdapter

    // The load that is running now. It is kept so a newer filter choice can cancel it.
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffInquiriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StaffInquiryAdapter { inquiry ->
            val intent = Intent(requireContext(), StaffInquiryDetailActivity::class.java)
            intent.putExtra(StaffInquiryDetailActivity.EXTRA_INQUIRY_ID, inquiry.inquiryId)
            startActivity(intent)
        }
        binding.rvInquiries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInquiries.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.brand_brick)
        binding.swipeRefresh.setOnRefreshListener { loadInquiries() }
        binding.btnRetry.setOnClickListener { loadInquiries() }

        // Choosing another chip loads the list again with the new filter.
        binding.chipGroup.setOnCheckedStateChangeListener { _, _ -> loadInquiries() }
    }

    // Loads every time the screen comes back into view, so changes made on the
    // details screen show straight away.
    override fun onResume() {
        super.onResume()
        loadInquiries()
    }

    // The status the API expects for the selected chip. Null means "All".
    private fun selectedStatus(): String? = when (binding.chipGroup.checkedChipId) {
        R.id.chipOpen -> "Open"
        R.id.chipInProgress -> "InProgress"
        R.id.chipResolved -> "Resolved"
        R.id.chipClosed -> "Closed"
        else -> null
    }

    private fun loadInquiries() {
        binding.swipeRefresh.isRefreshing = true
        binding.errorLayout.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        // Cancels an older load that is still running, so its late answer
        // cannot replace the newer list.
        loadJob?.cancel()

        val status = selectedStatus()

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.getService(requireContext()).getStaffInquiries(status)
                val inquiries = response.body()

                when {
                    response.code() == 401 ->
                        requireContext().returnToLogin("Your login has expired. Please log in again.")

                    response.isSuccessful && inquiries != null -> {
                        adapter.submit(inquiries)
                        if (inquiries.isEmpty()) {
                            binding.tvEmpty.visibility = View.VISIBLE
                        }
                    }

                    else -> showError(ApiErrors.messageFrom(response))
                }
            } catch (e: CancellationException) {
                // The screen closed or a newer load started. Nothing needs to be shown.
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Network problem while loading inquiries", e)
                showError("Could not reach the server. Check the internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected problem while loading inquiries", e)
                showError("Something went wrong. Please try again.")
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.errorLayout.visibility = View.VISIBLE
    }

    // The binding is cleared when the screen's view is destroyed, to avoid memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "StaffInquiriesFragment"
    }
}