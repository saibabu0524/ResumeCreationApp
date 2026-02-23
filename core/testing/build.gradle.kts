plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.testing")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.testing"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.analytics)

    implementation(libs.bundles.coroutines)
    implementation(libs.coroutines.test)
    implementation(libs.hilt.testing)
    implementation(libs.truth)
    implementation(libs.turbine)
    implementation(libs.junit5.api)
}
