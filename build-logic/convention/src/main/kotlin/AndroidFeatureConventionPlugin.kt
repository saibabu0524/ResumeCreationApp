import com.softsuave.resumecreationapp.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for all feature modules (:feature:*).
 *
 * Combines the library plugin with Hilt, Compose, Navigation,
 * ViewModel, Kotlin Serialization (for type-safe routes),
 * and standard feature testing dependencies.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("resumecreationapp.android.library")
                apply("resumecreationapp.android.compose")
                apply("resumecreationapp.android.hilt")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            dependencies {
                // Lifecycle + ViewModel
                add("implementation", libs.findBundle("lifecycle").get())

                // Navigation
                add("implementation", libs.findLibrary("navigation-compose").get())

                // Kotlin Serialization (for type-safe @Serializable routes)
                add("implementation", libs.findLibrary("kotlin-serialization-json").get())

                // Coroutines
                add("implementation", libs.findBundle("coroutines").get())

                // Testing
                add("testImplementation", libs.findBundle("testing").get())
                add("testRuntimeOnly", libs.findLibrary("junit5-engine").get())
                add("androidTestImplementation", libs.findBundle("android-testing").get())
            }
        }
    }
}
