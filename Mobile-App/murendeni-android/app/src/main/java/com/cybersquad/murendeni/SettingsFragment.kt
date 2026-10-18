package com.cybersquad.murendeni

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.util.returnToLogin

// Temporary screen. The real settings screen replaces this file in the next step.
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val session = SessionManager(context)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val info = TextView(context).apply {
            text = "${session.getName()}\nSigned in as ${session.getRole()}"
            gravity = Gravity.CENTER
        }

        val logout = Button(context).apply {
            text = "Log out"
            setOnClickListener { context.returnToLogin() }
        }

        layout.addView(info)
        layout.addView(logout)
        return layout
    }
}