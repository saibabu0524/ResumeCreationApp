import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.softsuave.resumecreationapp.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
    compileOnly(libs.junit5.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "resumecreationapp.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "resumecreationapp.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "resumecreationapp.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "resumecreationapp.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "resumecreationapp.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "resumecreationapp.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "resumecreationapp.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("androidWork") {
            id = "resumecreationapp.android.work"
            implementationClass = "AndroidWorkConventionPlugin"
        }
        register("androidTesting") {
            id = "resumecreationapp.android.testing"
            implementationClass = "AndroidTestingConventionPlugin"
        }
    }
}
