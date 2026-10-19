package com.cybersquad.murendeni

import com.cybersquad.murendeni.util.DateFormatter
import com.cybersquad.murendeni.util.FileUtils
import com.cybersquad.murendeni.util.StatusUi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

// Tests for the status labels and colours shown on the cards.
class StatusUiTest {

    @Test
    fun projectLabel_showsInProgressWithASpace() {
        assertEquals("In Progress", StatusUi.projectLabel("InProgress"))
    }

    @Test
    fun projectLabel_keepsOtherStatusesUnchanged() {
        assertEquals("Review", StatusUi.projectLabel("Review"))
        assertEquals("Completed", StatusUi.projectLabel("Completed"))
    }

    @Test
    fun projectColor_isDifferentForEachStatus() {
        val colors = listOf("Inquiry", "InProgress", "Review", "Completed")
            .map { StatusUi.projectColor(it) }

        assertEquals(4, colors.toSet().size)
    }

    @Test
    fun fileLabel_capitalisesTheFirstLetter() {
        assertEquals("Draft", StatusUi.fileLabel("draft"))
        assertEquals("Approved", StatusUi.fileLabel("approved"))
    }

    @Test
    fun inquiryLabel_showsInProgressWithASpace() {
        assertEquals("In Progress", StatusUi.inquiryLabel("InProgress"))
        assertEquals("Open", StatusUi.inquiryLabel("Open"))
    }

    @Test
    fun inquiryColor_resolvedAndClosedLookDifferent() {
        assertNotEquals(StatusUi.inquiryColor("Resolved"), StatusUi.inquiryColor("Closed"))
    }
}

// Tests for the file size and date text shown on screen.
class FormattingTest {

    private val originalLocale: Locale = Locale.getDefault()

    // English is set first, so the expected text does not depend on the language of the computer running the tests.
    @Before
    fun useEnglish() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLanguage() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun formatSize_showsKilobytesBelowOneMegabyte() {
        assertEquals("2 KB", FileUtils.formatSize(2048))
    }

    @Test
    fun formatSize_neverShowsZeroKilobytes() {
        assertEquals("1 KB", FileUtils.formatSize(10))
    }

    @Test
    fun formatSize_showsMegabytesWithOneDecimal() {
        assertEquals("1.0 MB", FileUtils.formatSize(1_048_576))
        assertEquals("2.4 MB", FileUtils.formatSize(2_516_582))
    }

    @Test
    fun dateFormatter_showsTheDateInShortForm() {
        assertEquals("18 Oct 2026", DateFormatter.format("2026-10-18T12:00:00Z"))
    }

    @Test
    fun dateFormatter_returnsTheOriginalTextWhenItIsNotADate() {
        assertEquals("not a date", DateFormatter.format("not a date"))
    }
}