pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// ─── Remote Build Cache ───
// Configuration step for consuming projects: Uncomment and configure this block
// to enable a remote Gradle build cache for faster CI builds.
// buildCache {
//     local {
//         isEnabled = true
//     }
//     remote<HttpBuildCache> {
//         url = uri("https://your-build-cache-server.example.com/cache/")
//         isPush = System.getenv("CI") != null
//         credentials {
//             username = System.getenv("BUILD_CACHE_USER") ?: ""
//             password = System.getenv("BUILD_CACHE_PASSWORD") ?: ""
//         }
//     }
// }

rootProject.name = "ResumeCreationApp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":benchmark")
include(":baselineprofile")

// Core modules
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:ui")
include(":core:analytics")
include(":core:feature-flags")
include(":core:notifications")
include(":core:work")
include(":core:testing")

// Feature modules
include(":feature:auth")
include(":feature:home")
include(":feature:settings")
include(":feature:profile")
