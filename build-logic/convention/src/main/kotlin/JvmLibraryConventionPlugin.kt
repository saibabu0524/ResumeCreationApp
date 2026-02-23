import com.softsuave.resumecreationapp.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for pure Kotlin JVM modules (no Android).
 *
 * Used for :core:domain and any other modules that must remain
 * Android-free (pure Kotlin).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("java-library")
                apply("org.jetbrains.kotlin.jvm")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            configureKotlinJvm()
        }
    }
}
