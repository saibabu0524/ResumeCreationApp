import com.softsuave.resumecreationapp.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for WorkManager-enabled modules (:core:work).
 *
 * Adds WorkManager runtime, KTX, and testing dependencies.
 */
class AndroidWorkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("implementation", libs.findLibrary("work-runtime-ktx").get())
                add("androidTestImplementation", libs.findLibrary("work-testing").get())
            }
        }
    }
}
