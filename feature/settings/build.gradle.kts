plugins {
    id("resumecreationapp.android.feature")
    alias(libs.plugins.junit5)
}

android {
    namespace = "com.softsuave.resumecreationapp.feature.settings"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.featureFlags)
    implementation(projects.core.datastore)

    testImplementation(projects.core.testing)
}
