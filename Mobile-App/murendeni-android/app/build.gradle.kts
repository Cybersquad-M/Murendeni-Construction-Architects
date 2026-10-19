plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.cybersquad.murendeni"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.cybersquad.murendeni"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true   // Gives each layout a generated class, so views are reached without findViewById.
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Library for calling the website API.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // Turns the API's JSON into Kotlin objects.
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Logs each API call, which helps when something fails.
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Runs API calls in the background without freezing the screen.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Loads and shows images from web addresses.
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Pull-down-to-refresh gesture for the project list.
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Scrolling list used for the projects.
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // A real JSON library for unit tests. The Android one is only an empty stub on a build machine.
    testImplementation("org.json:json:20240303")

}