plugins {
    id("resumecreationapp.android.feature")
}

android {
    namespace = "com.softsuave.resumecreationapp.feature.ats"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.ui)

    // PDF handling
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.okhttp.core)
}
