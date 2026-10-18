package com.cybersquad.murendeni.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Turns the API's UTC dates (like "2026-10-18T09:12:00Z") into dates in the phone's local time.
object DateFormatter {

    private val formatter = DateTimeFormatter
        .ofPattern("dd MMM yyyy", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    fun format(isoDate: String): String {
        return try {
            formatter.format(Instant.parse(isoDate))
        } catch (e: Exception) {
            // The raw text is shown if the format is unexpected, instead of crashing.
            isoDate
        }
    }
}