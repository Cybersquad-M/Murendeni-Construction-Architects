package com.cybersquad.murendeni

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cybersquad.murendeni.api.ApiClient
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.FragmentSettingsBinding
import com.cybersquad.murendeni.util.returnToLogin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

// The Settings tab: shows who is logged in and links to the account screens.
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isClient = SessionManager(requireContext()).getRole() == "Client"

        // Only clients can edit their own details. Staff details are changed by an Admin on the website.
        if (!isClient) {
            binding.rowEditProfile.visibility = View.GONE
            binding.dividerRows.visibility = View.GONE
            binding.tvStaffNote.visibility = View.VISIBLE
        }

        binding.rowEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        binding.rowChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        binding.btnHelp.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }

        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    // Refreshes the details every time the screen comes back into view,
    // so a name changed on the edit screen shows straight away.
    override fun onResume() {
        super.onResume()

        val session = SessionManager(requireContext())
        binding.tvName.text = session.getName()
        binding.tvEmail.text = session.getEmail()
        binding.tvRole.text = session.getRole()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ -> logOut() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Tells the server first, so the logout is recorded in the system log, and then clears the
    // saved login. If the phone is offline or the server is slow, the user is still logged out.
    private fun logOut() {
        val appContext = requireContext().applicationContext
        binding.btnLogout.isEnabled = false // stops a second tap while the server is told

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // The saved token is still needed for this call, so it is sent before clearing the session.
                withTimeoutOrNull(3000) { ApiClient.getService(appContext).logout() }
            } catch (e: IOException) {
                Log.d("SettingsFragment", "Could not tell the server about the logout: ${e.message}")
            } finally {
                appContext.returnToLogin()
            }
        }
    }

    // The binding is cleared when the screen's view is destroyed, to avoid memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}