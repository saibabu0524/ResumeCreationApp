plugins {
    id("resumecreationapp.android.feature")
}

android {
    namespace = "com.softsuave.resumecreationapp.feature.resume"
}

dependencies {
    // Navigation inside feature
    implementation(libs.navigation.compose)
    implementation(libs.kotlin.serialization.json)
    
    // Core dependencies
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.ui)
    
    testImplementation(projects.core.testing)
    
    // PDF handling logic
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.okhttp.core)
}
