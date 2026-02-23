plugins {
    id("resumecreationapp.android.library")
    id("resumecreationapp.android.hilt")
}

android {
    namespace = "com.softsuave.resumecreationapp.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)

    implementation(libs.bundles.networking)
    implementation(libs.bundles.coroutines)

    debugImplementation(libs.chucker.library)
    releaseImplementation(libs.chucker.no.op)

    testImplementation(libs.bundles.testing)
}
