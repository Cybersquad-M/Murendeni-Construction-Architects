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
// Adds padding at the top only, so content stays below the status bar.
// Used where the bottom edge is already handled by the bottom navigation bar.
fun View.applyTopSystemBarPadding() {
    val startTop = paddingTop

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(view.paddingLeft, startTop + bars.top, view.paddingRight, view.paddingBottom)
        insets
    }
}