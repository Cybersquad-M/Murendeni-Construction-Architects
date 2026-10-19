# Murendeni Construction Architects

## Task 1 Project Repository

This repository contains the project planning, website design, mobile application design, prototype files and final documentation for the Murendeni Construction Architects project.

---

## Project Overview

The Murendeni Construction Architects project focuses on designing and developing a professional digital solution for a construction and architectural business.

The project includes a website and mobile application concept designed to provide users with access to relevant information, services and project-related functionality.

---

## Project Objectives

The main objectives of this project are to:

- Develop a professional website design.
- Design a logical mobile application.
- Create clear website and mobile application navigation.
- Produce detailed wireframes.
- Develop an initial prototype.
- Maintain project documentation.
- Use GitHub for version control and collaboration.
- Ensure all project deliverables are organised and accessible.

---

## Project Components

### Website

The website component includes:

- Website sitemap
- Website wireframes
- Website prototype
- HTML
- CSS
- JavaScript
- Website assets

### Mobile Application

The mobile application component includes:

- Mobile app sitemap
- Mobile app wireframes
- Screen designs
- Navigation flow
- Mobile application assets

### Documentation

The documentation includes:

- Updated project plan
- Website design documentation
- Mobile app design documentation
- Final Task 1 documentation
- References

---

## Repository Structure

```text
Murendeni-Construction-Architects/
│
├── Website/
│   ├── HTML/
│   ├── CSS/
│   ├── JavaScript/
│   └── Assets/
│
├── Mobile-App/
│   ├── Wireframes/
│   ├── Sitemap/
│   └── Assets/
│
├── Documentation/
│   ├── Project-Plan/
│   ├── Website-Design/
│   ├── Mobile-App-Design/
│   └── Final-Task-1/
│
├── Prototype/
│
└── README.md

# Admin login details are as follows:
 admin@murendeni.co.za
 password: Passw0rd!

##Part 2

Murendeni Construction Architects: Mobile App (Task 2)

An Android app that lets clients follow their projects, and lets designers and sales staff manage them from a phone. It talks to a REST API added to the existing Murendeni website, so both share one database and one set of rules.

Team: <team name> Members: <member names and student numbers> Module: <module code>, Task 2

Features
	Client	Designer	Sales
Projects	Own projects	Assigned projects	All projects, create new
Inquiries	Send and follow own	Hidden	Filter, assign, change status, create client account
Project details	View and download files	Change status, upload files	Change status
Settings	Edit profile, change password	Change password	Change password

Every role also has an in-app Help screen. Admin accounts are web-only.

Technology
App: Kotlin, Android (Material 3, View Binding), Retrofit/OkHttp, Gson, Coroutines, Glide
API: ASP.NET Core, Entity Framework Core, JWT authentication with role policies, Swagger
Testing and CI: JUnit 4, GitHub Actions
Hosting: Azure
Repository layout
Murendeni-Construction-Architects/
├── .github/workflows/     # android.yml (build + test), website.yml (optional)
├── Website/               # ASP.NET Core site and the /api endpoints
├── Mobile-App/
│   └── murendeni-android/ # The Android project
└── Documentation/         # User guide (PDF)
Running the project

API: open Website/, set the connection string and JWT settings in appsettings.json, run, and open /swagger to try the endpoints.

App: open Mobile-App/murendeni-android in Android Studio, let Gradle sync, check the API base URL in api/ApiClient.kt, then press Run. Requires JDK 17.

Test accounts

Role	Email	Password
Client	apitest@test.com	#Password123
Designer	design@murendeni.co.za	Welcome4068!
Sales	sales@murendeni.co.za 	Welcome8749!
Testing

Run the unit tests (no phone needed):

./gradlew testDebugUnitTest

They cover status labels and colours, date and file size formatting, API error messages, JSON model mapping, and role-based help content. The staff API endpoints were also tested by hand in Swagger for each role.

Continuous integration

.github/workflows/android.yml runs on every push to main that changes Mobile-App/. It runs the unit tests, builds the debug APK, and saves the test report and APK as artifacts. Open the Actions tab and scroll to Artifacts on the latest run to download them.

Documentation

The full user guide is in the Documentation folder as a PDF. It covers installing, logging in, every screen for each role, troubleshooting, a status glossary, and a table of every button and label.

Known limitations
Uploaded files are stored on the web server (wwwroot/uploads) until they move to Azure Blob Storage.
Passwords cannot be reset yet.
Single sign-on (SSO) and the seed data script are still to come.
