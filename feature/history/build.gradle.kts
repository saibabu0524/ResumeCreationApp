plugins {
    id("resumecreationapp.android.feature")
}

android {
    namespace = "com.softsuave.resumecreationapp.feature.history"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)

    testImplementation(projects.core.testing)
}
