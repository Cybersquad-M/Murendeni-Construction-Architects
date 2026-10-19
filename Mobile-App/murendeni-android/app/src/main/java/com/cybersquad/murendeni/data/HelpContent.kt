package com.cybersquad.murendeni.data

// One question with its answer.
data class HelpItem(val question: String, val answer: String)

// A group of questions under one heading.
data class HelpSection(val title: String, val items: List<HelpItem>)

// The text of the in-app help. Every role sees the general topics and the troubleshooting
// topics, plus the topics written for its own role.
object HelpContent {

    private const val PHONE = "+27 11 123 4567"
    private const val EMAIL = "info@murendeni.co.za"

    // An empty role means nobody is logged in, for example when help is opened from the login screen.
    fun forRole(role: String): List<HelpSection> {
        val sections = mutableListOf(gettingStarted())

        when (role) {
            "Client" -> sections.add(clientTopics())
            "Designer" -> sections.add(designerTopics())
            "Sales" -> sections.add(salesTopics())
        }

        sections.add(troubleshooting())
        return sections
    }

    private fun gettingStarted() = HelpSection(
        "Getting started",
        listOf(
            HelpItem(
                "How do I log in?",
                "Type your email and password on the login screen and tap Login. " +
                        "Clients can create an account with the Register link. " +
                        "Staff accounts are created by an Admin on the website, so staff cannot register in the app."
            ),
            HelpItem(
                "How do I change my password?",
                "Open the Settings tab and tap Change password. Enter your current password and the new one twice, then tap Update password."
            ),
            HelpItem(
                "How do I log out?",
                "Open the Settings tab and tap Log out. A login lasts 24 hours, after which the app asks you to log in again."
            ),
            HelpItem(
                "I am an Admin. Why can I not log in?",
                "Admin accounts can only be used on the website."
            )
        )
    )

    private fun clientTopics() = HelpSection(
        "Using the app as a client",
        listOf(
            HelpItem(
                "How do I see my projects?",
                "Open the Projects tab. Each card shows the status of the project. Tap a card to see its details."
            ),
            HelpItem(
                "What do the project statuses mean?",
                "Inquiry: your request was received and the project is being set up.\n\n" +
                        "In Progress: the design team is working on it.\n\n" +
                        "Review: the work is ready to be checked.\n\n" +
                        "Completed: the project is finished."
            ),
            HelpItem(
                "How do I view or download a design file?",
                "Open the project and scroll to Design Files. View opens the file in your browser or another app. " +
                        "Download saves it to your Downloads folder and shows a notification when it is done."
            ),
            HelpItem(
                "What do the file statuses mean?",
                "Draft is work in progress. Review is ready to be checked. Approved is checked and accepted. " +
                        "Archived is kept for records."
            ),
            HelpItem(
                "How do I send an inquiry?",
                "Open the Inquiries tab and tap New. Choose a service if you like, type your message and tap Send inquiry. " +
                        "The status starts as Open and changes as the team works on it."
            ),
            HelpItem(
                "How do I change my name or phone number?",
                "Open Settings and tap Edit profile. Change the details and tap Save changes. The email address cannot be changed."
            )
        )
    )

    private fun designerTopics() = HelpSection(
        "Using the app as a designer",
        listOf(
            HelpItem(
                "Which projects can I see?",
                "Only the projects assigned to you. Sales staff assign a designer when they create a project."
            ),
            HelpItem(
                "How do I update a project's status?",
                "Open the project, choose a new Status in the Update status box and tap Save status. The client sees the change."
            ),
            HelpItem(
                "How do I upload a design file?",
                "Open the project and tap Upload. Choose a file, check the title, pick a status and tap Upload file. " +
                        "Allowed types are PDF, PNG, JPG, JPEG, WEBP, DWG, DXF, RVT, SKP and ZIP, up to 25 MB."
            ),
            HelpItem(
                "Why is there no Inquiries tab?",
                "Inquiries and client accounts are handled by sales staff."
            ),
            HelpItem(
                "How do I change my name or email?",
                "Staff details are changed by an Admin on the website."
            )
        )
    )

    private fun salesTopics() = HelpSection(
        "Using the app as sales staff",
        listOf(
            HelpItem(
                "How do I deal with an inquiry?",
                "Open the Inquiries tab and use the filters at the top to narrow the list. Tap an inquiry, " +
                        "choose a status and who to assign it to, then tap Save changes."
            ),
            HelpItem(
                "How do I create a client account from an inquiry?",
                "Open an inquiry marked Guest and tap Create client account. A temporary password is shown once. " +
                        "Tap Copy password and give it to the client, who can change it in Settings."
            ),
            HelpItem(
                "How do I create a project?",
                "On the Projects tab tap New. Choose the client and a designer, enter a name and a type, then tap Create project. " +
                        "The project starts with the status Inquiry."
            ),
            HelpItem(
                "How do I change a project's designer or publish a project?",
                "Changing the designer later is done on the website under Manage Projects, then Edit. " +
                        "Publishing to the public portfolio is done by an Admin on the website."
            ),
            HelpItem(
                "How do I change a project's status?",
                "Open the project, choose a Status in the Update status box and tap Save status."
            ),
            HelpItem(
                "Why can I not upload files?",
                "Design files are uploaded by designers."
            )
        )
    )

    private fun troubleshooting() = HelpSection(
        "Problems",
        listOf(
            HelpItem(
                "The app says it cannot reach the server.",
                "Check your internet connection. The server can take up to a minute to wake up after a quiet period, " +
                        "so wait a moment and tap Try again."
            ),
            HelpItem(
                "I forgot my password.",
                "Passwords can be reset in the app by clicking change password in the settings page. Contact Murendeni on $PHONE or $EMAIL for help."
            ),
            HelpItem(
                "The app asked me to log in again.",
                "A login lasts 24 hours. Log in again with the same details."
            ),
            HelpItem(
                "A list is empty.",
                "Pull the list down to refresh it. Clients see projects only after the team has created one for them."
            ),
            HelpItem(
                "A file will not upload or open.",
                "Check that the file type is allowed and that it is under 25 MB. " +
                        "If a file will not open, use Download and open it from your Downloads folder."
            ),
            HelpItem(
                "Who do I contact?",
                "Phone $PHONE or email $EMAIL."
            )
        )
    )
}