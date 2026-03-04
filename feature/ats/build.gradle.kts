plugins {
    id("resumecreationapp.android.feature")
}

android {
    namespace = "com.softsuave.resumecreationapp.feature.ats"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.ui)

    testImplementation(projects.core.testing)

    // PDF handling
    implementation("androidx.core:core-ktx:1.13.1")
}

