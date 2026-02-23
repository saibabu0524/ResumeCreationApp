import com.android.build.gradle.LibraryExtension
import com.softsuave.resumecreationapp.configureKotlinAndroid
import com.softsuave.resumecreationapp.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for all Android library modules (core:*, etc.).
 *
 * Applies Android Library + Kotlin Android plugins and shared configuration.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)

                defaultConfig {
                    consumerProguardFiles("consumer-rules.pro")
                }

                testOptions {
                    unitTests {
                        isIncludeAndroidResources = true
                    }
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }
            }

            dependencies {
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("timber").get())
            }
        }
    }
}
