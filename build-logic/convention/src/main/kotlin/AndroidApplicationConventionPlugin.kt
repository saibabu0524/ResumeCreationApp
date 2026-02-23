import com.android.build.api.dsl.ApplicationExtension
import com.softsuave.resumecreationapp.configureKotlinAndroid
import com.softsuave.resumecreationapp.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for the :app module.
 *
 * Applies Android Application plugin, Kotlin Android, Kotlin Serialization,
 * configures product flavors (dev, staging, prod), build types, and signing.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)

                defaultConfig {
                    targetSdk = 35
                    versionCode = 1
                    versionName = "1.0.0"

                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }

                buildFeatures {
                    buildConfig = true
                }

                // ─── Product Flavors ───
                flavorDimensions += "environment"

                productFlavors {
                    create("dev") {
                        dimension = "environment"
                        applicationIdSuffix = ".dev"
                        versionNameSuffix = "-dev"
                        manifestPlaceholders["appName"] = "ResumeCreationApp DEV"

                        val devBaseUrl = project.findProperty("DEV_BASE_URL") as? String
                            ?: "https://346c-2402-e280-213a-15c-bc91-c533-3048-8f04.ngrok-free.app/"
                        buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
                    }

                    create("staging") {
                        dimension = "environment"
                        applicationIdSuffix = ".staging"
                        versionNameSuffix = "-staging"
                        manifestPlaceholders["appName"] = "ResumeCreationApp STG"

                        val stagingBaseUrl = project.findProperty("STAGING_BASE_URL") as? String
                            ?: "https://staging-api.example.com/v1/"
                        buildConfigField("String", "BASE_URL", "\"$stagingBaseUrl\"")
                    }

                    create("prod") {
                        dimension = "environment"
                        manifestPlaceholders["appName"] = "ResumeCreationApp"

                        val prodBaseUrl = project.findProperty("PROD_BASE_URL") as? String
                            ?: "https://api.example.com/v1/"
                        buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
                    }
                }

                // ─── Build Types ───
                buildTypes {
                    debug {
                        isDebuggable = true
                        isMinifyEnabled = false
                        applicationIdSuffix = ".debug"
                    }

                    release {
                        isDebuggable = false
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )

                        // Signing config — reads from local.properties or CI env vars
                        // Uncomment and configure when ready:
                        // signingConfig = signingConfigs.getByName("release")
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                        excludes += "/META-INF/INDEX.LIST"
                        excludes += "/META-INF/DEPENDENCIES"
                    }
                }
            }
        }
    }
}
