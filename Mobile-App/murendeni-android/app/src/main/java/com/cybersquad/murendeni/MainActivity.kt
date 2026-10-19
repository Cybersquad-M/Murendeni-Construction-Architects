package com.cybersquad.murendeni

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityMainBinding
import com.cybersquad.murendeni.util.applyTopSystemBarPadding

// Holds the bottom tab bar and shows the screen for the selected tab.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keeps the content below the status bar. The tab bar handles the bottom edge itself.
        binding.fragmentContainer.applyTopSystemBarPadding()

        val role = SessionManager(this).getRole()

        // Clients send and follow their inquiries. Sales staff manage all of them.
        // Designers do not use this tab.
        val hasInquiriesTab = role == "Client" || role == "Sales"
        binding.bottomNav.menu.findItem(R.id.nav_inquiries).isVisible = hasInquiriesTab

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_projects -> showFragment(ProjectsFragment())
                R.id.nav_inquiries -> {
                    if (role == "Sales") {
                        showFragment(StaffInquiriesFragment())
                    } else {
                        showFragment(InquiriesFragment())
                    }
                }
                R.id.nav_settings -> showFragment(SettingsFragment())
            }
            true
        }

        // Only on the first start: opens the Projects tab. After a screen rotation,
        // Android restores the screen that was showing.
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_projects
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}