plugins {
    id("resumecreationapp.jvm.library")
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.kotlin.serialization.json)
    api(libs.javax.inject)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
