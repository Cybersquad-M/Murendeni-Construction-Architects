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
import com.cybersquad.murendeni.databinding.FragmentInquiriesBinding
import com.cybersquad.murendeni.util.returnToLogin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

// The Inquiries tab. Shows the client's own inquiries and a button to send a new one.
class InquiriesFragment : Fragment() {

    private var _binding: FragmentInquiriesBinding? = null
    private val binding get() = _binding!!

    private val adapter = InquiryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInquiriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvInquiries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInquiries.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.brand_brick)
        binding.swipeRefresh.setOnRefreshListener { loadInquiries() }
        binding.btnRetry.setOnClickListener { loadInquiries() }
        binding.btnNew.setOnClickListener {
            startActivity(Intent(requireContext(), NewInquiryActivity::class.java))
        }
    }

    // Loads every time the screen comes back into view, so a newly sent inquiry shows straight away.
    override fun onResume() {
        super.onResume()
        loadInquiries()
    }

    private fun loadInquiries() {
        binding.swipeRefresh.isRefreshing = true
        binding.errorLayout.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.getService(requireContext()).getInquiries()
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
                // The screen was closed while loading, for example when the user switched tabs.
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
        private const val TAG = "InquiriesFragment"
    }
}