plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.compose")
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.softsuave.resumecreationapp.core.ui"
}

dependencies {
    implementation(projects.core.common)

    // Image loading
    implementation(libs.coil.compose)

    // Adaptive navigation (bottom bar / rail / drawer)
    implementation(libs.adaptive.navigation.suite)

    // Lifecycle (collectAsStateWithLifecycle)
    implementation(libs.bundles.lifecycle)

    // Testing
    testImplementation(libs.bundles.testing)
    androidTestImplementation(libs.bundles.android.testing)
}
