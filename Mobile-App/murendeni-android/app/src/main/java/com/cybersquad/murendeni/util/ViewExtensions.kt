package com.cybersquad.murendeni.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Newer Android versions draw apps behind the status bar and navigation bar.
// This adds padding so content stays clear of both bars and of the keyboard.
fun View.applySystemBarPadding() {
    val startLeft = paddingLeft
    val startTop = paddingTop
    val startRight = paddingRight
    val startBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        view.setPadding(
            startLeft + bars.left,
            startTop + bars.top,
            startRight + bars.right,
            startBottom + bars.bottom
        )
        insets
    }
}