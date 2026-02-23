plugins {
    id("resumecreationapp.android.feature")
    alias(libs.plugins.junit5)
}

android {
    namespace = "com.softsuave.resumecreationapp.feature.home"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.featureFlags)

    testImplementation(projects.core.testing)
}
