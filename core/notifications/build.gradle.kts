plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.notifications"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.datastore)

    implementation(libs.bundles.coroutines)

    // Firebase messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // WorkManager (for token sync scheduling)
    implementation(libs.work.runtime.ktx)

    // App Startup (for notification channel initialization)
    implementation(libs.startup.runtime)

    testImplementation(libs.bundles.testing)
}
