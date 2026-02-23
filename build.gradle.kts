// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
}

// ─── Ktlint — Code Formatting ─────────────────────────────────────────────
// Applied to all subprojects for consistent formatting.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }
}

// ─── Detekt — Clean Architecture & Code Quality ───────────────────────────
// Enforces: Domain classes can't import Android SDK, features can't import each other.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
        parallel = true
        autoCorrect = false

        source.setFrom(
            "src/main/kotlin",
            "src/main/java",
        )
    }
}

// ─── Spotless — Auto-Formatting Wrapper ───────────────────────────────────
// Single task: ./gradlew spotlessApply to auto-format all Kotlin files.
apply(plugin = "com.diffplug.spotless")

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        trimTrailingWhitespace()
        endWithNewline()
        // Ktlint formatting with Android style
        ktlint("1.3.1")
            .setEditorConfigPath(rootProject.file(".editorconfig"))
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ─── Cookiecutter Template Sync ─────────────────────────────────────────────
// Single task: ./gradlew syncTemplate to copy main application changes into
// the android-cookiecutter-template output folder.
tasks.register<Exec>("syncTemplate") {
    group = "ResumeCreationApp"
    description = "Synchronizes changes from the Android application to the Cookiecutter template."
    commandLine("python3", "scripts/sync_cookiecutter.py")
    workingDir(rootProject.projectDir)
}
