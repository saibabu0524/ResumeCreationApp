plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.softsuave.resumecreationapp.baselineprofile"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.test.runner)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}

baselineProfile {
    useConnectedDevices = true
}
