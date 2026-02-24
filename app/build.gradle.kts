plugins {
    id("resumecreationapp.android.application")
    id("resumecreationapp.android.compose")
    id("resumecreationapp.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.dependency.guard)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.softsuave.resumecreationapp"
    
    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
    }
}

dependencies {
    // Core modules
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.featureFlags)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.notifications)
    implementation(projects.core.work)

    // Feature modules
    implementation(projects.feature.auth)
    implementation(projects.feature.home)
    implementation(projects.feature.settings)
    implementation(projects.feature.profile)
    implementation(projects.feature.resume)
    implementation(projects.feature.ats)

    // Navigation (type-safe routes)
    implementation(libs.navigation.compose)
    implementation(libs.kotlin.serialization.json)

    // Adaptive navigation
    implementation(libs.adaptive.navigation.suite)

    // Splash Screen
    implementation(libs.splash.screen)

    // App Startup
    implementation(libs.startup.runtime)

    // WorkManager + Hilt integration (for HiltWorkerFactory in Application)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Play services
    implementation(libs.play.app.update)
    implementation(libs.play.app.review)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // Debug tools
    debugImplementation(libs.bundles.debug)
    debugImplementation(libs.chucker.library)
    releaseImplementation(libs.chucker.no.op)

    // OkHttp logging — used directly in AppModule for HttpLoggingInterceptor
    implementation(libs.okhttp.logging)

    // Baseline Profiles
    implementation(libs.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))
}

// ─── Dependency Guard ─────────────────────────────────────────────────────
// Locks the full dependency tree to a baseline file in the repo.
// CI build fails on any unexpected dependency change (including transitive).
// Generate baseline: ./gradlew :app:dependencyGuardBaseline
// Verify: ./gradlew :app:dependencyGuard
dependencyGuard {
    configuration("prodReleaseRuntimeClasspath")
}
