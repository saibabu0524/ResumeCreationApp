plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.datastore"
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.bundles.datastore)
    implementation(libs.security.crypto)
    implementation(libs.bundles.coroutines)

    testImplementation(libs.bundles.testing)
}
