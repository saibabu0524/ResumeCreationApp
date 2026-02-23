plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.data"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.datastore)

    implementation(libs.bundles.coroutines)
    // Retrofit is needed directly for API service creation in RepositoryModule
    implementation(libs.bundles.networking)

    testImplementation(libs.bundles.testing)
}
