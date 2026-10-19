package com.cybersquad.murendeni

import com.cybersquad.murendeni.data.HelpContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests that each role sees the right help topics.
class HelpContentTest {

    private fun titles(role: String): List<String> = HelpContent.forRole(role).map { it.title }

    @Test
    fun visitorSeesOnlyTheGeneralAndProblemTopics() {
        assertEquals(listOf("Getting started", "Problems"), titles(""))
    }

    @Test
    fun clientSeesClientTopics() {
        assertTrue(titles("Client").contains("Using the app as a client"))
    }

    @Test
    fun designerSeesDesignerTopics() {
        assertTrue(titles("Designer").contains("Using the app as a designer"))
    }

    @Test
    fun salesSeesSalesTopics() {
        assertTrue(titles("Sales").contains("Using the app as sales staff"))
    }

    @Test
    fun roleTopicsAreNotMixedBetweenRoles() {
        assertFalse(titles("Client").contains("Using the app as a designer"))
        assertFalse(titles("Designer").contains("Using the app as sales staff"))
        assertFalse(titles("Sales").contains("Using the app as a client"))
    }

    @Test
    fun everyQuestionHasAnAnswer() {
        for (role in listOf("", "Client", "Designer", "Sales")) {
            for (section in HelpContent.forRole(role)) {
                for (item in section.items) {
                    assertTrue(item.question.isNotBlank())
                    assertTrue(item.answer.isNotBlank())
                }
            }
        }
    }
}