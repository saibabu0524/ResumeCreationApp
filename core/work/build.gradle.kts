plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
    id("resumecreationapp.android.work")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.work"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.domain)

    // Hilt WorkManager integration
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.bundles.coroutines)

    testImplementation(libs.bundles.testing)
}
