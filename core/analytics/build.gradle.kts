plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.analytics"
}

dependencies {
    implementation(libs.bundles.coroutines)

    // Firebase Analytics — leaf dependency, not exposed to feature modules
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
