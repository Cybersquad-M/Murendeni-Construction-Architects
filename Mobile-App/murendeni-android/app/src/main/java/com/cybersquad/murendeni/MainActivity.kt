package com.cybersquad.murendeni

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityMainBinding
import com.cybersquad.murendeni.util.applySystemBarPadding

// Temporary home screen. It shows who is logged in and lets the user log out.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        val session = SessionManager(this)
        binding.tvWelcome.text = "Welcome, ${session.getName()}"
        binding.tvRole.text = "Signed in as ${session.getRole()}"

        binding.btnLogout.setOnClickListener {
            session.clear()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}