package com.cybersquad.murendeni.util

// Turns status values from the API into text and colours for the screens.
object StatusUi {

    // "InProgress" is shown as "In Progress". Other statuses are shown as they are.
    fun projectLabel(status: String): String =
        if (status == "InProgress") "In Progress" else status

    fun projectColor(status: String): Int = when (status) {
        "InProgress" -> 0xFF3A5570.toInt()   // blue-grey
        "Review" -> 0xFF6B3FA0.toInt()       // purple
        "Completed" -> 0xFF1F6B3A.toInt()    // green
        else -> 0xFFB5573A.toInt()           // brick, used for "Inquiry"
    }

    // File statuses arrive in lower case ("draft"), so the first letter is capitalised.
    fun fileLabel(status: String): String =
        status.replaceFirstChar { it.uppercase() }

    fun fileColor(status: String): Int = when (status) {
        "review" -> 0xFF6B3FA0.toInt()
        "approved" -> 0xFF1F6B3A.toInt()
        "archived" -> 0xFF6B7280.toInt()
        else -> 0xFFB5573A.toInt()           // draft
    }
}