plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.featureflags"
}

dependencies {
    implementation(libs.bundles.coroutines)

    // Firebase Remote Config — leaf dependency, not exposed to feature modules
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
}
