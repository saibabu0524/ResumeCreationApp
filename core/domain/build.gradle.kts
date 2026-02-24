plugins {
    id("resumecreationapp.jvm.library")
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.kotlin.serialization.json)
    api(libs.javax.inject)

    // JVM-only test deps — do NOT add roborazzi/robolectric here (AAR, needs Android module)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
    testRuntimeOnly(libs.junit5.engine)
}
