package com.cybersquad.murendeni

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

// Runs once when the app starts.
class MurendeniApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Forces the light theme, so the colours look the same on every phone even when dark mode is on.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}