plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
    id("resumecreationapp.android.room")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.database"
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.bundles.coroutines)

    testImplementation(libs.bundles.testing)
}
