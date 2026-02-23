import com.softsuave.resumecreationapp.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for the :core:testing module.
 *
 * Adds the full test dependency stack: JUnit 4/5, MockK, Turbine,
 * coroutines-test, Compose UI testing, and Hilt testing.
 */
class AndroidTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                // Unit testing
                add("api", libs.findBundle("testing").get())

                // JUnit 5 engine
                add("api", libs.findLibrary("junit5-engine").get())

                // Coroutines test
                add("api", libs.findLibrary("coroutines-test").get())

                // Android testing
                add("api", libs.findBundle("android-testing").get())

                // Compose BOM (required for version-less Compose dependencies)
                add("api", platform(libs.findLibrary("compose-bom").get()))

                // Compose testing
                add("api", libs.findLibrary("compose-ui-test-junit4").get())
            }
        }
    }
}
