package com.cybersquad.murendeni

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.FragmentSettingsBinding
import com.cybersquad.murendeni.util.returnToLogin
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
            .setPositiveButton("Log out") { _, _ -> requireContext().returnToLogin() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // The binding is cleared when the screen's view is destroyed, to avoid memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}